package com.braemar.ceapi.websocket;

import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.WebSocketServer.ClientConnectedEvent;
import com.braemar.ceapi.websocket.WebSocketServer.ClientDisconnectedEvent;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketServerIntegrationTest {

    private static final int TEST_PORT = 19001;
    private Observable<ClientConnectedEvent> serverConnectEvents;
    private Observable<ClientDisconnectedEvent> serverDisconnectEvents;
    private WebSocketServer server;

    @BeforeEach
    void setUp() throws InterruptedException {
        serverConnectEvents = new Observable<>();
        serverDisconnectEvents = new Observable<>();
        server = makeServer(TEST_PORT, new Observable<>(), serverConnectEvents, serverDisconnectEvents);
        server.start();
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    // --- Message delivery (server → client) ---

    @Test
    void publish_deliversMessageToConnectedClient() throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        TestClient client = new TestClient(TEST_PORT, received);
        connectAndAwaitServerOpen(client);

        server.publish("[\"refresh\",\"TFM\",[[2003,\"51.11\"]]]");

        assertEquals("[\"refresh\",\"TFM\",[[2003,\"51.11\"]]]", received.poll(2, TimeUnit.SECONDS));
        client.closeBlocking();
    }

    @Test
    void publish_doesNotThrow_whenNoClientConnected() {
        assertDoesNotThrow(() -> server.publish("[\"update\",\"TFM\",[[2003,\"51.12\"]]]"));
    }

    // --- Command receipt (client → server) ---

    @Test
    void commandEvents_receivesCommandSentByClient() throws Exception {
        BlockingQueue<String> commands = new LinkedBlockingQueue<>();
        Observable<String> commandEvents = new Observable<>();
        Observable<ClientConnectedEvent> connectEvents = new Observable<>();
        Observable<ClientDisconnectedEvent> disconnectEvents = new Observable<>();
        commandEvents.subscribe(commands::add);

        server.stop();
        server = makeServer(TEST_PORT + 1, commandEvents, connectEvents, disconnectEvents);
        serverConnectEvents = connectEvents;
        serverDisconnectEvents = disconnectEvents;
        server.start();
        Thread.sleep(100);

        TestClient client = new TestClient(TEST_PORT + 1);
        connectAndAwaitServerOpen(client);

        client.send("resync");
        assertEquals("resync", commands.poll(2, TimeUnit.SECONDS));
        client.closeBlocking();
    }

    // --- Connect event ---

    @Test
    void clientConnectedEvent_raisedOnConnect() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        serverConnectEvents.subscribe(e -> latch.countDown());

        TestClient client = new TestClient(TEST_PORT);
        connectAndAwaitServerOpen(client);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "clientConnectedEvent not raised on connect");
        client.closeBlocking();
    }

    @Test
    void clientConnectedEvent_raisedAgainOnReconnect() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        serverConnectEvents.subscribe(e -> latch.countDown());

        TestClient client1 = new TestClient(TEST_PORT);
        connectAndAwaitServerOpen(client1);
        client1.closeBlocking();
        Thread.sleep(100);

        TestClient client2 = new TestClient(TEST_PORT);
        connectAndAwaitServerOpen(client2);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "clientConnectedEvent not raised on reconnect");
        client2.closeBlocking();
    }

    // --- Reconnect ---

    @Test
    void clientReconnect_serverAcceptsNewSession_andDeliversMessage() throws Exception {
        TestClient client1 = new TestClient(TEST_PORT);
        connectAndAwaitServerOpen(client1);
        client1.closeBlocking();
        Thread.sleep(100);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        TestClient client2 = new TestClient(TEST_PORT, received);
        connectAndAwaitServerOpen(client2);

        server.publish("[\"refresh\",\"TFM\",[[2003,\"51.11\"]]]");
        assertEquals("[\"refresh\",\"TFM\",[[2003,\"51.11\"]]]", received.poll(2, TimeUnit.SECONDS));
        client2.closeBlocking();
    }

    @Test
    void multipleClients_receiveMessages_andOnlyLastDisconnectRaisesEvent() throws Exception {
        BlockingQueue<String> firstMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> secondMessages = new LinkedBlockingQueue<>();
        CountDownLatch disconnected = new CountDownLatch(1);
        AtomicInteger connected = new AtomicInteger();
        serverDisconnectEvents.subscribe(event -> disconnected.countDown());
        serverConnectEvents.subscribe(event -> connected.incrementAndGet());

        TestClient firstClient = new TestClient(TEST_PORT, firstMessages);
        TestClient secondClient = new TestClient(TEST_PORT, secondMessages);
        connectAndAwaitServerOpen(firstClient);
        connectAndAwaitServerOpen(secondClient);
        assertEquals(1, connected.get(), "only the first client should start ICE");

        server.publish("[\"update\",\"TFM\",[[2003,\"51.12\"]]]");

        assertEquals("[\"update\",\"TFM\",[[2003,\"51.12\"]]]", firstMessages.poll(2, TimeUnit.SECONDS));
        assertEquals("[\"update\",\"TFM\",[[2003,\"51.12\"]]]", secondMessages.poll(2, TimeUnit.SECONDS));

        firstClient.closeBlocking();
        assertFalse(disconnected.await(200, TimeUnit.MILLISECONDS), "disconnect raised while another client remained");

        secondClient.closeBlocking();
        assertTrue(disconnected.await(2, TimeUnit.SECONDS), "last client disconnect was not raised");
    }

    // --- Helpers ---

    private void connectAndAwaitServerOpen(TestClient client) throws Exception {
        assertTrue(client.connectBlocking(), "WebSocket client failed to connect");
        // The client's handshake completes before the server's onOpen has registered it.
        long deadline = System.currentTimeMillis() + 2000;
        while (!server.hasClient() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(server.hasClient(), "Server did not register the client");
    }

    private static WebSocketServer makeServer(int port,
            Observable<String> commandEvents,
            Observable<ClientConnectedEvent> clientConnectedEvents,
            Observable<ClientDisconnectedEvent> clientDisconnectedEvents) {
        return new WebSocketServer(port, commandEvents, clientConnectedEvents, clientDisconnectedEvents);
    }

    static class TestClient extends WebSocketClient {

        private final BlockingQueue<String> received;

        TestClient(int port) {
            this(port, new LinkedBlockingQueue<>());
        }

        TestClient(int port, BlockingQueue<String> received) {
            super(URI.create("ws://localhost:" + port));
            this.received = received;
        }

        @Override public void onOpen(ServerHandshake h) {}
        @Override public void onMessage(String message) { received.add(message); }
        @Override public void onClose(int code, String reason, boolean remote) {}
        @Override public void onError(Exception ex) {}
    }
}