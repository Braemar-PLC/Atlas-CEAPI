package com.braemar.ceapi.integration;

import com.braemar.ceapi.config.Settings;
import com.braemar.ceapi.ice.IceConnectionManager;
import com.braemar.ceapi.ice.IceQuoteListener;
import com.braemar.ceapi.ice.QuoteReceivedEvent;
import com.braemar.ceapi.services.IceWebSocketBridge;
import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.MessageType;
import com.braemar.ceapi.websocket.WebSocketServer;
import com.braemar.ceapi.websocket.WebSocketServer.ClientConnectedEvent;
import com.braemar.ceapi.websocket.WebSocketServer.ClientDisconnectedEvent;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class IceIntegrationTest {

    private static final Logger logger = Logger.getLogger(IceIntegrationTest.class.getName());

    private IceWebSocketBridge bridge;
    private Observable<QuoteReceivedEvent> quoteEmitter;
    private Observable<ClientConnectedEvent> clientConnectedEvents;
    private Settings settings;

    @BeforeEach
    void setUp() throws Exception {
        settings = Settings.fromEnv();
        ResourceManagerFactory factory = ResourceManagerFactory.getFactory();
        List<String> symbols = List.of(settings.symbols.split(","));

        quoteEmitter = new Observable<>();
        clientConnectedEvents = new Observable<>();
        Observable<String> commandEvents = new Observable<>();
        Observable<ClientDisconnectedEvent> clientDisconnectedEvents = new Observable<>();
        Observable<StatusEvent> statusEmitter = new Observable<>();
        Observable<SymbolEvent> symbolEmitter = new Observable<>();

        IceQuoteListener listener = new IceQuoteListener(quoteEmitter, statusEmitter, symbolEmitter);
        IceConnectionManager iceManager = new IceConnectionManager(settings, factory, listener, symbols);
        WebSocketServer wsServer = new WebSocketServer(settings.wsPort, commandEvents,
                clientConnectedEvents, clientDisconnectedEvents);

        bridge = new IceWebSocketBridge(wsServer, iceManager, logger,
                commandEvents, clientConnectedEvents, clientDisconnectedEvents,
                quoteEmitter, statusEmitter, symbolEmitter);

        bridge.start();
        Thread.sleep(150);
    }

    @AfterEach
    void tearDown() throws Exception {
        bridge.stop();
        Thread.sleep(200);
    }

    // --- helpers ---

    private TestClient connectClient() throws Exception {
        CountDownLatch serverSawConnect = new CountDownLatch(1);
        clientConnectedEvents.subscribe(e -> serverSawConnect.countDown());
        TestClient client = new TestClient(settings.wsPort);
        client.connectBlocking(5, TimeUnit.SECONDS);
        assertTrue(serverSawConnect.await(5, TimeUnit.SECONDS), "Bridge did not observe client connect");
        return client;
    }

    private QuoteReceivedEvent awaitFirstQuote(BlockingQueue<QuoteReceivedEvent> queue,
                                                long timeout, TimeUnit unit) throws InterruptedException {
        QuoteReceivedEvent e = queue.poll(timeout, unit);
        assertNotNull(e, "No quote received within timeout");
        return e;
    }

    private static String format(QuoteReceivedEvent e, int maxFields) {
        if (e == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append(e.type).append(" ").append(e.symbol).append(" [");
        int n = Math.min(maxFields, e.fieldIds.length);
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(e.fieldIds[i]).append("=").append(e.fieldValues[i]);
        }
        if (e.fieldIds.length > n) sb.append(", ...");
        sb.append("]");
        return sb.toString();
    }

    // --- tests ---

    @Test
    @Timeout(60)
    void onConnect_clientReceivesQuotes() throws Exception {
        BlockingQueue<QuoteReceivedEvent> received = new LinkedBlockingQueue<>();
        quoteEmitter.subscribe(received::add);

        TestClient client = connectClient();

        QuoteReceivedEvent first = awaitFirstQuote(received, 45, TimeUnit.SECONDS);
        System.out.println("First quote: " + format(first, 6));

        assertNotNull(first.symbol);
        assertFalse(first.symbol.isBlank());
        assertTrue(first.fieldIds.length > 0);

        client.closeBlocking();
    }

    @Test
    @Timeout(75)
    void onReconnect_clientReceivesRefreshNotDeltas() throws Exception {
        BlockingQueue<QuoteReceivedEvent> received = new LinkedBlockingQueue<>();
        quoteEmitter.subscribe(received::add);

        // --- first connect: wait for initial data ---
        TestClient c1 = connectClient();

        QuoteReceivedEvent first = awaitFirstQuote(received, 45, TimeUnit.SECONDS);
        System.out.println("First after connect: " + format(first, 6));
        assertEquals(MessageType.REFRESH, first.type,
                "First message after connect should be REFRESH, got: " + first.type);

        // drain remaining initial messages
        Thread.sleep(1500);
        received.clear();

        // --- disconnect ---
        c1.closeBlocking();
        Thread.sleep(500);

        // --- reconnect ---
        TestClient c2 = connectClient();

        QuoteReceivedEvent afterReconnect = awaitFirstQuote(received, 45, TimeUnit.SECONDS);
        System.out.println("First after reconnect: " + format(afterReconnect, 6));
        assertEquals(MessageType.REFRESH, afterReconnect.type,
                "First message after reconnect should be REFRESH, got: " + afterReconnect.type);

        c2.closeBlocking();
    }

    // --- test client ---

    static class TestClient extends WebSocketClient {
        TestClient(int port) {
            super(URI.create("ws://localhost:" + port));
        }
        @Override public void onOpen(ServerHandshake h) {}
        @Override public void onMessage(String message) {}
        @Override public void onClose(int code, String reason, boolean remote) {}
        @Override public void onError(Exception ex) {}
    }
}