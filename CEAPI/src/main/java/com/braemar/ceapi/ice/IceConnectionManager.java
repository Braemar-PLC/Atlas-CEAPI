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
import java.util.List;
import java.util.logging.Logger;

/**
 * Opens one SDK session on client connection and closes it on explicit disconnect.
 * No application-managed reconnect timers; SDK recovery remains unchanged.
 */
public class IceConnectionManager {

    private static final Logger logger = Logger.getLogger(IceConnectionManager.class.getName());

    private final Settings settings;
    private final ResourceManagerFactory factory;
    private final IceQuoteListener listener;
    private final List<String> symbols;
    private final Observable<FeedStatusEvent> feedStatusEvents;

    private final Object lock = new Object();
    private QuoteManager quoteManager;
    private List<String> subscribed = List.of();
    private Runnable onConnected;
    private boolean connected;
    private int generation;
    private FeedState feedState = FeedState.DISCONNECTED;
    private String feedDetail = "No WebSocket client is connected";
    private String disconnectStatus;

    public IceConnectionManager(Settings settings,
            ResourceManagerFactory factory,
            IceQuoteListener listener,
            List<String> symbols) {
        this(settings, factory, listener, symbols, new Observable<>());
    }

    public IceConnectionManager(Settings settings,
            ResourceManagerFactory factory,
            IceQuoteListener listener,
            List<String> symbols,
            Observable<FeedStatusEvent> feedStatusEvents) {
        this.settings = settings;
        this.factory = factory;
        this.listener = listener;
        this.symbols = List.copyOf(symbols);
        this.feedStatusEvents = feedStatusEvents;
    }

    /**
     * Connects to ICE and keeps it connected until {@link #disconnect}. onConnected runs each time a session
     * opens (the first time and after every reconnect) - use it to subscribe.
     */
    public void connect(Runnable onConnected) {
        synchronized (lock) {
            this.onConnected = onConnected;
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
                    qm.disconnect();
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
            synchronized (lock) {
                if (gen == generation) {
                    feedState = FeedState.DISCONNECTED;
                    feedDetail = "Failed to connect to ICE: " + e.getMessage();
                }
            }
            publishStatus();
            teardown();
            throw new IllegalStateException("Failed to connect to ICE", e);
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
                    feedState = FeedState.LIVE;
                    feedDetail = "Connected to ICE";
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
                        + ". No application-managed reconnect is scheduled.");
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
    /**
     * Unsubscribes all symbols then resubscribes to force a fresh snapshot.
     * Safe to call only when already connected.
     */
    public void resync() {
        logger.info("Resync requested — unsubscribing and resubscribing all symbols");
        unsubscribeAll();
        subscribeAll();
    }

    /** Subscribes to the fixed operator-configured list. */
    public void subscribeAll() {
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
            connected = false;
            generation++;
            feedState = FeedState.DISCONNECTED;
            feedDetail = "No WebSocket client is connected";
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