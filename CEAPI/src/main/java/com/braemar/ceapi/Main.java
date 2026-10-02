
package com.braemar.ceapi;

import com.braemar.ceapi.config.Settings;
import com.braemar.ceapi.config.SymbolListLoader;
import com.braemar.ceapi.ice.IceConnectionManager;
import com.braemar.ceapi.ice.FeedStatusEvent;
import com.braemar.ceapi.ice.IceQuoteListener;
import com.braemar.ceapi.ice.QuoteReceivedEvent;
import com.braemar.ceapi.services.IceWebSocketBridge;
import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.WebSocketServer;
import com.braemar.ceapi.websocket.WebSocketServer.ClientConnectedEvent;
import com.braemar.ceapi.websocket.WebSocketServer.ClientDisconnectedEvent;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        Dotenv.configure()
                .ignoreIfMissing()
                .load()
                .entries()
                .forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        Settings settings = Settings.fromEnv();
        ResourceManagerFactory factory = ResourceManagerFactory.getFactory();

        var commandEvents = new Observable<String>();
        var clientConnectedEvents = new Observable<ClientConnectedEvent>();
        var clientDisconnectedEvents = new Observable<ClientDisconnectedEvent>();
        var quoteEventEmitter = new Observable<QuoteReceivedEvent>();
        var feedStatusEventEmitter = new Observable<FeedStatusEvent>();
        var statusEventEmitter = new Observable<StatusEvent>();
        var symbolEventEmitter = new Observable<SymbolEvent>();

        // 4) ICE listeners & manager (no connect here)
        var iceQuoteListener = new IceQuoteListener(quoteEventEmitter, statusEventEmitter, symbolEventEmitter);
        var scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ceapi-health");
            thread.setDaemon(true);
            return thread;
        });
        var iceManager = new IceConnectionManager(settings, factory, iceQuoteListener,
                new SymbolListLoader(settings).get(), feedStatusEventEmitter);
        scheduler.scheduleAtFixedRate(iceManager::publishStatus, 0, 5, TimeUnit.SECONDS);

        // 5) WebSocket server (binds the port; receives client/command events)
        var wsServer = new WebSocketServer(
                settings.wsPort,
                commandEvents,
                clientConnectedEvents,
                clientDisconnectedEvents);

        // 6) Service (subscribes to Observables; owns lifecycle)
        var service = new IceWebSocketBridge(
                wsServer,
                iceManager,
                logger,
                commandEvents,
                clientConnectedEvents,
                clientDisconnectedEvents,
                quoteEventEmitter,
                feedStatusEventEmitter,
                statusEventEmitter,
                symbolEventEmitter);

        // 7) Run
        service.start();
        Runtime.getRuntime().addShutdownHook(new Thread(service::stop));

        // Keep JVM alive
        Thread.currentThread().join();
    }
}
