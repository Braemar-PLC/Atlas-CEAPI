package com.braemar.ceapi.services;

import com.braemar.ceapi.ice.IceConnectionManager;
import com.braemar.ceapi.ice.FeedStatusEvent;
import com.braemar.ceapi.ice.FeedState;
import com.braemar.ceapi.ice.QuoteReceivedEvent;
import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.MessageType;
import com.braemar.ceapi.websocket.WebSocketServer;
import com.braemar.ceapi.websocket.WebSocketServer.ClientConnectedEvent;
import com.braemar.ceapi.websocket.WebSocketServer.ClientDisconnectedEvent;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class IceWebSocketBridgeTest {

    @Mock
    private WebSocketServer wsServer;
    @Mock
    private IceConnectionManager iceManager;

    private Observable<String> commandEvents;
    private Observable<ClientConnectedEvent> clientConnectedEvents;
    private Observable<ClientDisconnectedEvent> clientDisconnectedEvents;
    private Observable<QuoteReceivedEvent> quoteReceivedEvents;
    private Observable<FeedStatusEvent> feedStatusEvents;
    private Observable<StatusEvent> statusEvents;
    private Observable<SymbolEvent> symbolEvents;

    private IceWebSocketBridge service;
    private static final Logger logger = Logger.getLogger(IceWebSocketBridgeTest.class.getName());

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        commandEvents = new Observable<>();
        clientConnectedEvents = new Observable<>();
        clientDisconnectedEvents = new Observable<>();
        quoteReceivedEvents = new Observable<>();
        feedStatusEvents = new Observable<>();
        statusEvents = new Observable<>();
        symbolEvents = new Observable<>();

        service = new IceWebSocketBridge(
                wsServer, iceManager, logger,
                commandEvents,
                clientConnectedEvents,
                clientDisconnectedEvents,
                quoteReceivedEvents,
                feedStatusEvents,
                statusEvents,
                symbolEvents);

    }

    // --- lifecycle ---

    @Test
    void start_startsWsServer() {
        var order = inOrder(wsServer, iceManager);
        service.start();
        order.verify(wsServer).start();
    }

    @Test
    void stop_stopsWsServerAndDisconnectsIce() {
        service.stop();
        verify(wsServer).stop();
        verify(iceManager).disconnect();
    }

    @Test
    void stop_stopsWsServerBeforeDisconnectingIce() {
        var order = inOrder(wsServer, iceManager);
        service.stop();
        order.verify(wsServer).stop();
        order.verify(iceManager).disconnect();
    }

    // --- onClientConnectedEvent ---

    @Test
    void onClientConnected_connectsIceAndSchedulesSubscription() {
        // New behaviour: no automatic resync on client connect.
        clientConnectedEvents.raise(new ClientConnectedEvent());
        verify(iceManager, never()).resync();
        // The expected calls are:
        verify(iceManager).connect( any(Runnable.class));

    }

    // --- onCommandEvent ---

    @Test
    void onCommand_resync_triggersResync() {
        commandEvents.raise("resync");
        verify(iceManager).resync();
    }

    @Test
    void onCommand_unknown_doesNotTriggerResync() {
        commandEvents.raise("garbage");
        verify(iceManager, never()).resync();
    }

    // --- onQuoteReceivedEvent ---

    @Test
    void onQuoteReceived_refresh_publishesRefreshMessage() {
        var event = new QuoteReceivedEvent("TFM", new short[] { 2003 }, new String[] { "51.11" }, MessageType.REFRESH);
        quoteReceivedEvents.raise(event);
        verify(wsServer).publish(argThat(msg -> msg.contains("\"refresh\"")));
    }

    @Test
    void onQuoteReceived_update_publishesUpdateMessage() {
        var event = new QuoteReceivedEvent("TFM", new short[] { 2003 }, new String[] { "51.12" }, MessageType.UPDATE);
        quoteReceivedEvents.raise(event);
        verify(wsServer).publish(argThat(msg -> msg.contains("\"update\"")));
    }

    @Test
    void onQuoteReceived_publishesCorrectSymbol() {
        var event = new QuoteReceivedEvent("GWM", new short[] { 2003 }, new String[] { "99.0" }, MessageType.REFRESH);
        quoteReceivedEvents.raise(event);
        verify(wsServer).publish(argThat(msg -> msg.contains("\"GWM\"")));
    }

    @Test
    void onFeedStatus_publishesStructuredStatusMessage() {
        feedStatusEvents.raise(new FeedStatusEvent(
                FeedState.AUTHENTICATION_FAILED,
                4,
                Instant.parse("2026-09-30T13:00:00Z"),
                "ICE rejected credentials",
                0));

        verify(wsServer).publish(argThat(msg ->
                msg.contains("\"AUTHENTICATION_FAILED\"")
                        && msg.contains("\"generation\":4")));
    }

    @Test
    void start_doesNotConnectIce() {
        service.start();
        verify(iceManager, never()).connect(any(Runnable.class));
    }

    @Test
    void clientConnect_triggersIceConnect() {
        service.start();
        clientConnectedEvents.raise(new ClientConnectedEvent());
        verify(iceManager).connect(any(Runnable.class));
    }

    @Test
    void onClientDisconnected_triggersIceDisconnect() {
        when(wsServer.openClientCount()).thenReturn(0);
        clientDisconnectedEvents.raise(new WebSocketServer.ClientDisconnectedEvent());
        verify(iceManager).disconnect();
    }

    @Test
    void onClientDisconnected_keepsIceConnectedWhileAnotherClientRemains() {
        when(wsServer.openClientCount()).thenReturn(1);

        clientDisconnectedEvents.raise(new WebSocketServer.ClientDisconnectedEvent());

        verify(iceManager, never()).disconnect();
    }
}