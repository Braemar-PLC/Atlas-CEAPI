package com.braemar.ceapi.ice;

import com.braemar.ceapi.config.Settings;
import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.beans.SockType;
import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;

import java.util.List;
import java.util.logging.Logger;

public class IceConnectionManager {

    private static final Logger logger = Logger.getLogger(IceConnectionManager.class.getName());

    private final Settings settings;
    private final ResourceManagerFactory factory;
    private final IceQuoteListener listener;
    private final List<String> symbols;

    private QuoteManager quoteManager;

    public IceConnectionManager(Settings settings,
            ResourceManagerFactory factory,
            IceQuoteListener listener,
            List<String> symbols) {
        this.settings = settings;
        this.factory = factory;
        this.listener = listener;
        this.symbols = symbols;
    }

    /**
     * Connects to ICE. The onConnected callback is invoked once the connection
     * is established — use it to subscribe or perform any post-connect work.
     * This is the only way to sequence work after connection; there are no flags
     * or deferred queues.
     */
    public void connect(Runnable onConnected) {
        try {
            disconnect();
            quoteManager = factory.createQuoteManager();

            ConnectionSettings cs = new ConnectionSettings(
                    settings.iceHost,
                    settings.iceUsername,
                    settings.icePassword,
                    SockType.SOCKTYPE_LEGACY);

                    quoteManager.connect(cs, new ConnectionListener() {
                @Override
                public void onConnected(ConnectionEvent e) {
                    logger.info("ICE connected: " + e.getConnectedHost());
                    onConnected.run();
                }

                @Override
                public void onConnecting(ConnectionEvent e) {
                    logger.info("ICE connecting...");
                }

                @Override
                public void onDisconnected(ConnectionEvent e) {
                    logger.warning("ICE disconnected");
                }

                @Override
                public void onDisconnecting(ConnectionEvent e) {
                    logger.info("ICE disconnecting: " + e.getStatusString());
                }

                @Override
                public void onError(ConnectionEvent e) {
                    logger.severe("ICE error: " + e.getStatusString());
                }
            });

        } catch (Exception e) {
            logger.severe("Failed to connect to ICE: " + e.getMessage());
            throw new RuntimeException(e);
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

    public void subscribeAll() {
        for (String symbol : symbols) {
            try {
                quoteManager.subscribe(symbol, listener);
                logger.info("Subscribed: " + symbol);
            } catch (Exception e) {
                logger.warning("Failed to subscribe to " + symbol + ": " + e.getMessage());
            }
        }
    }

    public void unsubscribeAll() {
        if (quoteManager == null) return;
        for (String symbol : symbols) {
            try {
                quoteManager.unsubscribe(symbol, listener);
            } catch (Exception e) {
                logger.warning("Failed to unsubscribe " + symbol + ": " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (quoteManager != null) {
            try {
                unsubscribeAll();
                quoteManager.disconnect();
                logger.info("Disconnected from ICE");
            } catch (Exception e) {
                logger.warning("Error disconnecting: " + e.getMessage());
            }
        }
    }
}