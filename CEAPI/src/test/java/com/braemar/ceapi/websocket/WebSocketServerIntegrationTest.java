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

import static org.junit.jupiter.api.Assertions.*;

class WebSocketServerIntegrationTest {

    private static final int TEST_PORT = 19001;
    private Observable<ClientConnectedEvent> serverConnectEvents;
    private WebSocketServer server;

    @BeforeEach
    void setUp() throws InterruptedException {
        serverConnectEvents = new Observable<>();
        server = makeServer(TEST_PORT, new Observable<>(), serverConnectEvents);
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
    commandEvents.subscribe(commands::add);

    server.stop();
    server = makeServer(TEST_PORT + 1, commandEvents, connectEvents);
    serverConnectEvents = connectEvents;
    server.start();
    Thread.sleep(100);

    CountDownLatch serverReady = new CountDownLatch(1);
    connectEvents.subscribe(e -> serverReady.countDown());

    TestClient client = new TestClient(TEST_PORT + 1);
    client.connectBlocking();
    assertTrue(serverReady.await(2, TimeUnit.SECONDS), "Server onOpen never fired");

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

    // --- Helpers ---

    private void connectAndAwaitServerOpen(TestClient client) throws Exception {
        connectAndAwaitServerOpen(client, TEST_PORT);
    }

    private void connectAndAwaitServerOpen(TestClient client, int port) throws Exception {
        CountDownLatch serverReady = new CountDownLatch(1);
        serverConnectEvents.subscribe(e -> serverReady.countDown());
        client.connectBlocking();
        assertTrue(serverReady.await(2, TimeUnit.SECONDS), "Server onOpen never fired");
    }

    private static WebSocketServer makeServer(int port,
            Observable<String> commandEvents,
            Observable<ClientConnectedEvent> clientConnectedEvents) {
        return new WebSocketServer(port, commandEvents, clientConnectedEvents, new Observable<>());
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