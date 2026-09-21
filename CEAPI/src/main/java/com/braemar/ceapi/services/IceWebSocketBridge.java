package com.braemar.ceapi.services;

import com.braemar.ceapi.ice.IceConnectionManager;
import com.braemar.ceapi.ice.QuoteReceivedEvent;
import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.CommandParser;
import com.braemar.ceapi.websocket.MessageBuilder;
import com.braemar.ceapi.websocket.MessageType;
import com.braemar.ceapi.websocket.WebSocketServer;
import com.braemar.ceapi.websocket.WebSocketServer.ClientConnectedEvent;
import com.braemar.ceapi.websocket.WebSocketServer.ClientDisconnectedEvent;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;

import java.util.logging.Logger;

/**
 * Bridges the WebSocket server and the ICE connection manager.
 *
 * Lifecycle:
 * - ICE is connected only when a WebSocket client is present.
 * - On client connect: connect ICE and subscribe. ICE will deliver a full
 * snapshot (onResponse) followed by streaming updates (onUpdate).
 * - On client disconnect: disconnect ICE. No point streaming when nobody
 * is listening.
 * - On client reconnect: full ICE disconnect/reconnect cycle guarantees
 * a fresh snapshot, at the cost of ~1-2s boot time. Acceptable because
 * the client is a web server maintaining its own state.
 * - On explicit resync command: unsubscribe/resubscribe to force a fresh
 * snapshot without disconnecting ICE.
 */
public class IceWebSocketBridge {

    private final WebSocketServer wsServer;
    private final IceConnectionManager iceManager;
    private final Logger logger;

    public IceWebSocketBridge(WebSocketServer wsServer, IceConnectionManager iceManager, Logger logger,
            Observable<String> commandEvent,
            Observable<ClientConnectedEvent> clientConnectedEvent,
            Observable<ClientDisconnectedEvent> clientDisconnectedEvent,
            Observable<QuoteReceivedEvent> quoteReceivedEvent,
            Observable<StatusEvent> statusEvent,
            Observable<SymbolEvent> symbolEvent) {
        this.wsServer = wsServer;
        this.iceManager = iceManager;
        this.logger = logger;

        commandEvent.subscribe(this::onCommandEvent);
        clientConnectedEvent.subscribe(this::onClientConnectedEvent);
        clientDisconnectedEvent.subscribe(this::onClientDisconnectedEvent);
        quoteReceivedEvent.subscribe(this::onQuoteReceivedEvent);
        statusEvent.subscribe(this::onStatusEvent);
        symbolEvent.subscribe(this::onSymbolEvent);
    }

    /**
     * Starts the WebSocket server. ICE is not connected here — we wait for
     * a client to connect before opening the ICE connection.
     */
    public void start() {
        logger.info("Starting CEAPI bridge - waiting for WebSocket client");
        wsServer.start();
    }

    /**
     * Stops the bridge. Disconnects ICE if connected, then stops the WebSocket
     * server.
     */
    public void stop() {
        logger.info("Shutting down CEAPI bridge");
        wsServer.stop();
        iceManager.disconnect();
    }

    // --- WebSocket events ---

    /**
     * Client connected. Connect to ICE and request subscriptions.
     * ICE will deliver onResponse (full snapshot) for each symbol, then
     * onUpdate (deltas) as prices change.
     */
    private void onClientConnectedEvent(ClientConnectedEvent event) {
        logger.info("WebSocket client connected: connecting to ICE");
        iceManager.connect(iceManager::subscribeAll);

    }

    /**
     * Client disconnected. Tear down ICE — no point receiving data with
     * nobody to send it to. Next connect will get a fresh snapshot.
     */
    private void onClientDisconnectedEvent(ClientDisconnectedEvent event) {
        logger.info("WebSocket client disconnected — disconnecting ICE");
        iceManager.disconnect();
    }

    /**
     * Explicit resync command from client. Forces unsubscribe/resubscribe
     * to get a fresh snapshot without a full ICE disconnect cycle.
     */
    private void onCommandEvent(String command) {
        switch (CommandParser.parse(command)) {
            case RESYNC -> {
                logger.info("Resync command received");
                iceManager.resync();
            }
            default -> logger.warning("Unknown command: " + command);
        }
    }

    // --- ICE events ---

    /**
     * Quote received from ICE. Build the wire message and publish to the
     * WebSocket client. REFRESH = full snapshot field set, UPDATE = changed
     * fields only.
     */
    private void onQuoteReceivedEvent(QuoteReceivedEvent event) {
        String message = event.type == MessageType.REFRESH
                ? MessageBuilder.refresh(event.symbol, event.fieldIds, event.fieldValues)
                : MessageBuilder.update(event.symbol, event.fieldIds, event.fieldValues);
        wsServer.publish(message);
    }

    private void onStatusEvent(StatusEvent e) {
        logger.warning("ICE status event: " + e);
    }

    private void onSymbolEvent(SymbolEvent e) {
        logger.info("ICE symbol event: " + e);
    }
}