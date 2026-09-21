
package com.braemar.ceapi.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import com.braemar.ceapi.utility.Observable;

/**
 * WebSocket server for the CEAPI relay.
 *
 * Built on Java-WebSocket (org.java_websocket), a battle-tested RFC 6455
 * implementation. This class owns no protocol logic — it only wires the
 * library's callbacks to application-level interfaces.
 *
 * Exactly one client connection is expected (the Web API). If a second client
 * connects it replaces the tracked session; the previous one is closed.
 *
 * Lifecycle:
 * 1. Construct with port and emitters.
 * 2. Subscribe to emitters externally.
 * 3. Call start().
 * 4. Call stop() to shut down cleanly.
 */
public class WebSocketServer extends org.java_websocket.server.WebSocketServer
        implements QuotePublisher {

    private static final Logger log = Logger.getLogger(WebSocketServer.class.getName());

    /** Marker payload for client-connected event (no fields). */
    public static final class ClientConnectedEvent {
    }

    public static final class ClientDisconnectedEvent {
    }

    private final Observable<String> commandEvents;
    private final Observable<ClientConnectedEvent> clientConnectedEvents;
    private final Observable<ClientDisconnectedEvent> clientDisconnectedEvents;

    private volatile WebSocket currentClient;

    public WebSocketServer(int port,
            Observable<String> commandEvents,
            Observable<ClientConnectedEvent> clientConnectedEvents,
            Observable<ClientDisconnectedEvent> clientDisconnectedEvents) {
        super(new InetSocketAddress(port));
        this.commandEvents = commandEvents;
        this.clientConnectedEvents = clientConnectedEvents;
        this.clientDisconnectedEvents = clientDisconnectedEvents;
        setReuseAddr(true);
    }

    /**
     * Starts the server.
     * The library starts the server in a daemon thread — this method returns
     * immediately; use stop() to shut down.
     *
     * @throws IllegalStateException if emitters are missing.
     */
    @Override
    public void start() {
        super.start();
        log.info("WebSocket server listening on port " + getPort());
    }

    /**
     * Stops the server, closing any active client connection.
     * Blocks up to 1 second for the server thread to exit.
     */
    public void stop() {
        try {
            super.stop(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warning("Interrupted while stopping WebSocket server");
        }
    }

    // --- QuotePublisher ---

    /** Sends a message to the currently connected client. No-op if none. */
    @Override
    public void publish(String message) {
        WebSocket client = currentClient;
        if (client != null && client.isOpen()) {

            System.out.println(message);

            client.send(message);
        }
    }

    /** Returns true if a client is currently connected and open. */
    public boolean hasClient() {
        WebSocket client = currentClient;
        return client != null && client.isOpen();
    }

    // --- Java-WebSocket callbacks ---

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("Client connected: " + conn.getRemoteSocketAddress());

        // Close any previous session — only one client is expected
        WebSocket previous = currentClient;
        if (previous != null && previous.isOpen()) {
            log.info("Replacing previous client connection");
            previous.close();
        }
        currentClient = conn;

        // Emit client-connected event
        clientConnectedEvents.raise(new ClientConnectedEvent());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("Client disconnected (code=" + code + ", remote=" + remote + ")");
        if (currentClient == conn) {
            currentClient = null;
        }

        clientDisconnectedEvents.raise(new ClientDisconnectedEvent());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.fine("Received command: " + message);
        commandEvents.raise(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String addr = (conn != null) ? conn.getRemoteSocketAddress().toString() : "unknown";
        log.severe("WebSocket error from " + addr + ": " + ex.getMessage());
    }

    @Override
    public void onStart() {
        log.info("WebSocket server started successfully on port " + getPort());
    }
}
