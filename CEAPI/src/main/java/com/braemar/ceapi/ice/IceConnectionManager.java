package com.braemar.ceapi.ice;

import com.braemar.ceapi.config.Settings;
import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import com.braemar.ceapi.utility.Observable;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Owns the ICE session. While a Web API client wants data ({@link #connect} until {@link #disconnect}), a session
 * that ICE drops or that fails to open is reopened with backoff, so the relay never has to be restarted by hand.
 * The symbol list is asked for on every (re)connect and on {@link #refreshSymbols}, so it follows the screens.
 */
public class IceConnectionManager {

    private static final Logger logger = Logger.getLogger(IceConnectionManager.class.getName());

    /** Seconds to wait before each reopen attempt; the last one repeats. The first is short because a dropped session
     * means blank grids for a trader, and ICE usually accepts an immediate reopen. */
    static final long[] BACKOFF_SECONDS = {1, 5, 10, 30, 60, 120};

    private final Settings settings;
    private final ResourceManagerFactory factory;
    private final IceQuoteListener listener;
    private final Supplier<List<String>> symbolSource;
    private final ScheduledExecutorService scheduler;
    private final Observable<FeedStatusEvent> feedStatusEvents;

    private final Object lock = new Object();
    private QuoteManager quoteManager;
    private List<String> subscribed = List.of();
    private Runnable onConnected;
    private boolean wanted;
    private boolean connected;
    private int generation;
    private int attempt;
    private ScheduledFuture<?> pendingReconnect;
    private FeedState feedState = FeedState.DISCONNECTED;
    private String feedDetail = "No WebSocket client is connected";
    private String disconnectStatus;

    public IceConnectionManager(Settings settings,
            ResourceManagerFactory factory,
            IceQuoteListener listener,
            List<String> symbols) {
        this(settings, factory, listener, () -> symbols, defaultScheduler(), new Observable<>());
    }

    public IceConnectionManager(Settings settings,
            ResourceManagerFactory factory,
            IceQuoteListener listener,
            Supplier<List<String>> symbolSource,
            ScheduledExecutorService scheduler) {
        this(settings, factory, listener, symbolSource, scheduler, new Observable<>());
    }

    public IceConnectionManager(Settings settings,
            ResourceManagerFactory factory,
            IceQuoteListener listener,
            Supplier<List<String>> symbolSource,
            ScheduledExecutorService scheduler,
            Observable<FeedStatusEvent> feedStatusEvents) {
        this.settings = settings;
        this.factory = factory;
        this.listener = listener;
        this.symbolSource = symbolSource;
        this.scheduler = scheduler;
        this.feedStatusEvents = feedStatusEvents;
    }

    public static ScheduledExecutorService defaultScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ice-reconnect");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Connects to ICE and keeps it connected until {@link #disconnect}. onConnected runs each time a session
     * opens (the first time and after every reconnect) - use it to subscribe.
     */
    public void connect(Runnable onConnected) {
        synchronized (lock) {
            wanted = true;
            this.onConnected = onConnected;
            attempt = 0;
            cancelPendingReconnect();
        }
        open();
    }

    /** Whether ICE has confirmed the current session. */
    public boolean isConnected() {
        synchronized (lock) {
            return connected;
        }
    }

    private void open() {
        int gen;
        synchronized (lock) {
            gen = ++generation;
            connected = false;
            disconnectStatus = null;
            feedState = FeedState.CONNECTING;
            feedDetail = "Connecting to ICE";
        }
        publishStatus();
        teardown();
        try {
            QuoteManager qm = factory.createQuoteManager();
            synchronized (lock) {
                if (gen != generation) {
                    return;
                }
                quoteManager = qm;
            }
            ConnectionSettings cs = new ConnectionSettings(
                    settings.iceHost,
                    settings.iceUsername,
                    settings.icePassword,
                    SockType.SOCKTYPE_LEGACY);
            qm.connect(cs, listenerFor(gen));
        } catch (Exception e) {
            logger.severe("Failed to connect to ICE: " + e);
            scheduleReconnect(gen);
        }
    }

    private ConnectionListener listenerFor(int gen) {
        return new ConnectionListener() {
            @Override
            public void onConnected(ConnectionEvent e) {
                Runnable callback;
                synchronized (lock) {
                    if (gen != generation) {
                        return;
                    }
                    connected = true;
                    disconnectStatus = null;
                    attempt = 0;
                    feedState = FeedState.LIVE;
                    feedDetail = "Connected to ICE";
                    cancelPendingReconnect();
                    callback = onConnected;
                }
                logger.info("ICE connected: host=" + e.getConnectedHost() + ", generation=" + gen);
                publishStatus();
                if (callback != null) {
                    callback.run();
                }
            }

            @Override
            public void onConnecting(ConnectionEvent e) {
                FeedState state;
                synchronized (lock) {
                    if (gen == generation) {
                        state = disconnectStatus == null ? FeedState.CONNECTING : FeedState.RECONNECTING;
                        feedState = state;
                        feedDetail = state == FeedState.CONNECTING
                                ? "Connecting to ICE"
                                : "ICE reconnecting after: " + disconnectStatus;
                    } else {
                        return;
                    }
                }
                logger.info(state == FeedState.CONNECTING ? "ICE connecting..." : "ICE reconnecting...");
                publishStatus();
            }

            @Override
            public void onDisconnected(ConnectionEvent e) {
                String status = statusOf(e);
                int subscribedCount;
                String finalStatus;
                synchronized (lock) {
                    if (gen != generation) {
                        return;
                    }
                    connected = false;
                    if (disconnectStatus == null) {
                        disconnectStatus = status;
                    }
                    feedState = isNoReconnectStatus(disconnectStatus)
                            ? FeedState.DISCONNECTED
                            : FeedState.RECONNECTING;
                    feedDetail = "ICE disconnected: " + disconnectStatus;
                    finalStatus = disconnectStatus;
                    subscribedCount = subscribed.size();
                }
                logger.warning("ICE disconnected: status=" + finalStatus
                        + ", generation=" + gen
                        + ", subscribedSymbols=" + subscribedCount
                        + ". JStandard automatic recovery remains responsible for reconnecting.");
                publishStatus();
            }

            @Override
            public void onDisconnecting(ConnectionEvent e) {
                String status = statusOf(e);
                int subscribedCount;
                synchronized (lock) {
                    if (gen != generation) {
                        return;
                    }
                    connected = false;
                    disconnectStatus = status;
                    feedState = isNoReconnectStatus(status)
                            ? FeedState.DISCONNECTED
                            : FeedState.RECONNECTING;
                    feedDetail = "ICE disconnecting: " + status;
                    subscribedCount = subscribed.size();
                }
                logger.warning("ICE disconnecting: status=" + status
                        + ", generation=" + gen
                        + ", subscribedSymbols=" + subscribedCount);
                publishStatus();
            }

            @Override
            public void onError(ConnectionEvent e) {
                String status = statusOf(e);
                synchronized (lock) {
                    if (gen == generation) {
                        connected = false;
                        disconnectStatus = status;
                        boolean authenticationFailed = status != null
                                && status.contains("DBCAPI_ERROR_WRONG_USERNAMEPASSWORD");
                        feedState = authenticationFailed
                                ? FeedState.AUTHENTICATION_FAILED
                                : isNoReconnectStatus(status)
                                        ? FeedState.DISCONNECTED
                                        : FeedState.RECONNECTING;
                        feedDetail = authenticationFailed
                                ? "ICE rejected the configured credentials"
                                : "ICE connection error: " + status;
                    }
                }
                logger.severe("ICE error: status=" + status + ", generation=" + gen);
                publishStatus();
            }
        };
    }

    private static String statusOf(ConnectionEvent event) {
        String status = event.getStatusString();
        return status == null || status.isBlank() ? "UNKNOWN" : status;
    }

    private static boolean isNoReconnectStatus(String status) {
        return status.contains("DBCAPI_ERROR_MUST_UPGRADE")
                || status.contains("DBCAPI_ERROR_WRONG_USERNAMEPASSWORD")
                || status.contains("DBCAPI_ERROR_NOT_ENTITLED")
                || status.contains("DBCAPI_ERROR_ADDRESS_CHANGE")
                || status.contains("DBCAPI_ERROR_NOT_ENDOFDAY");
    }

    /** Retries failures that occur before JStandard owns a live or recovering connection. */
    private void scheduleReconnect(int gen) {
        synchronized (lock) {
            if (!wanted || gen != generation || pendingReconnect != null) {
                return;
            }
            long delay = BACKOFF_SECONDS[Math.min(attempt, BACKOFF_SECONDS.length - 1)];
            attempt++;
            logger.warning("Reopening the ICE session in " + delay + "s");
            pendingReconnect = scheduler.schedule(() -> {
                synchronized (lock) {
                    pendingReconnect = null;
                    // ICE may have restored the session itself in the meantime.
                    if (!wanted || gen != generation || connected) {
                        return;
                    }
                }
                open();
            }, delay, TimeUnit.SECONDS);
        }
    }

    private void cancelPendingReconnect() {
        if (pendingReconnect != null) {
            pendingReconnect.cancel(false);
            pendingReconnect = null;
        }
    }

    /**
     * Unsubscribes all symbols then resubscribes to force a fresh snapshot.
     * Safe to call only when already connected.
     */
    public void resync() {
        logger.info("Resync requested — unsubscribing and resubscribing all symbols");
        unsubscribeAll();
        subscribeAll();
    }

    /** Asks for the current symbol list and subscribes to every one of them. */
    public void subscribeAll() {
        List<String> symbols = symbolSource.get();
        QuoteManager qm;
        synchronized (lock) {
            qm = quoteManager;
        }
        if (qm == null) {
            return;
        }
        int ok = subscribe(qm, symbols);
        synchronized (lock) {
            subscribed = symbols;
        }
        logger.info("Subscribed to " + ok + " of " + symbols.size() + " symbols");
        publishStatus();
    }

    /**
     * Asks for the symbol list again and changes only the difference: new symbols (a contract rolled in, an
     * option chain moved with its future) are subscribed, ones no longer listed are dropped. No-op while offline.
     */
    public void refreshSymbols() {
        QuoteManager qm;
        List<String> before;
        synchronized (lock) {
            if (!connected) {
                return;
            }
            qm = quoteManager;
            before = subscribed;
        }
        List<String> now = symbolSource.get();
        if (now.isEmpty()) {
            return;
        }
        Set<String> nowSet = new HashSet<>(now);
        Set<String> beforeSet = new HashSet<>(before);
        List<String> added = now.stream().filter(s -> !beforeSet.contains(s)).toList();
        List<String> removed = before.stream().filter(s -> !nowSet.contains(s)).toList();
        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }
        unsubscribe(qm, removed);
        subscribe(qm, added);
        synchronized (lock) {
            if (quoteManager == qm) {
                subscribed = now;
            }
        }
        logger.info("Symbol list refreshed: " + added.size() + " added, " + removed.size() + " removed");
    }

    public void unsubscribeAll() {
        QuoteManager qm;
        List<String> symbols;
        synchronized (lock) {
            qm = quoteManager;
            symbols = subscribed;
            subscribed = List.of();
        }
        if (qm != null) {
            unsubscribe(qm, symbols);
        }
    }

    /** Closes the session on purpose: no reconnect until the next {@link #connect}. */
    public void disconnect() {
        synchronized (lock) {
            wanted = false;
            connected = false;
            generation++;
            feedState = FeedState.DISCONNECTED;
            feedDetail = "No WebSocket client is connected";
            cancelPendingReconnect();
        }
        publishStatus();
        teardown();
    }

    /** Publishes the current session state. Called periodically so consumers can detect a silent relay failure. */
    public void publishStatus() {
        FeedStatusEvent event;
        synchronized (lock) {
            event = new FeedStatusEvent(
                    feedState,
                    generation,
                    Instant.now(),
                    feedDetail,
                    subscribed.size());
        }
        feedStatusEvents.raise(event);
    }

    private void teardown() {
        QuoteManager qm;
        synchronized (lock) {
            qm = quoteManager;
            quoteManager = null;
        }
        if (qm == null) {
            return;
        }
        try {
            List<String> symbols;
            synchronized (lock) {
                symbols = subscribed;
                subscribed = List.of();
            }
            unsubscribe(qm, symbols);
            qm.disconnect();
            logger.info("Disconnected from ICE");
        } catch (Exception e) {
            logger.warning("Error disconnecting: " + e.getMessage());
        }
    }

    private int subscribe(QuoteManager qm, List<String> symbols) {
        int ok = 0;
        for (String symbol : symbols) {
            try {
                qm.subscribe(symbol, listener);
                ok++;
            } catch (Exception e) {
                logger.warning("Failed to subscribe to " + symbol + ": " + e.getMessage());
            }
        }
        return ok;
    }

    private void unsubscribe(QuoteManager qm, List<String> symbols) {
        for (String symbol : symbols) {
            try {
                qm.unsubscribe(symbol, listener);
            } catch (Exception e) {
                logger.warning("Failed to unsubscribe " + symbol + ": " + e.getMessage());
            }
        }
    }
}