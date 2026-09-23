
package com.braemar.ceapi.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import com.braemar.ceapi.utility.Observable;

/**
 * WebSocket server for the CEAPI relay.
 *
 * Built on Java-WebSocket (org.java_websocket), a battle-tested RFC 6455
 * implementation. This class owns no protocol logic — it only wires the
 * library's callbacks to application-level interfaces.
 *
 * The Web API may have more than one worker during an App Service deployment
 * or when scaled out, so each active connection receives relay messages.
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

    private final Set<WebSocket> clients = ConcurrentHashMap.newKeySet();

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

    /** Sends a message to every connected client. No-op if none. */
    @Override
    public void publish(String message) {
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }

    /** Returns true if at least one client is currently connected and open. */
    public boolean hasClient() {
        return clients.stream().anyMatch(WebSocket::isOpen);
    }

    // --- Java-WebSocket callbacks ---

    @Override
    public synchronized void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("Client connected: " + conn.getRemoteSocketAddress());

        if (clients.add(conn) && clients.size() == 1) {
            clientConnectedEvents.raise(new ClientConnectedEvent());
        }
    }

    @Override
    public synchronized void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("Client disconnected (code=" + code + ", remote=" + remote + ")");

        if (clients.remove(conn) && clients.isEmpty()) {
            clientDisconnectedEvents.raise(new ClientDisconnectedEvent());
        }
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
