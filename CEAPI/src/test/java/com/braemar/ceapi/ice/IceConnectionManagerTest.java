package com.braemar.ceapi.ice;

import com.braemar.ceapi.config.Settings;
import com.esignal.jstandard.beans.ConnectionSettings;
import com.esignal.jstandard.event.ConnectionEvent;
import com.esignal.jstandard.event.ConnectionListener;
import com.esignal.jstandard.managers.QuoteManager;
import com.esignal.jstandard.managers.ResourceManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IceConnectionManagerTest {

    @Mock private ResourceManagerFactory factory;
    @Mock private QuoteManager quoteManager;
    @Mock private IceQuoteListener listener;
    @Mock private ConnectionEvent connectionEvent;

    private IceConnectionManager manager;
    private Settings settings;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(factory.createQuoteManager()).thenReturn(quoteManager);
        settings = makeSettings("icehost", "user", "pass", "SYM1,SYM2", 9001);
        manager = new IceConnectionManager(settings, factory, listener, List.of("SYM1", "SYM2"));
    }

    // --- connect() ---

    @Test
    void connect_createsQuoteManager() throws Exception {
        manager.connect(mock(Runnable.class));
        verify(factory).createQuoteManager();
    }

    @Test
    void connect_callsQuoteManagerWithCorrectSettings() throws Exception {
        manager.connect(mock(Runnable.class));

        ArgumentCaptor<ConnectionSettings> cap = ArgumentCaptor.forClass(ConnectionSettings.class);
        verify(quoteManager).connect(cap.capture(), any(ConnectionListener.class));

        assertEquals("icehost", cap.getValue().getHost());
        assertEquals("user",    cap.getValue().getUsername());
        assertEquals("pass",    cap.getValue().getPassword());
    }

    @Test
    void connect_registersConnectionListener() throws Exception {
        manager.connect(mock(Runnable.class));
        verify(quoteManager).connect(any(), any(ConnectionListener.class));
    }

    @Test
    void connect_firesCallbackWhenIceConnects() throws Exception {
        Runnable callback = mock(Runnable.class);
        manager.connect(callback);

        fireOnConnected();

        verify(callback).run();
    }

    @Test
    void connect_doesNotFireCallbackBeforeIceConnects() throws Exception {
        Runnable callback = mock(Runnable.class);
        manager.connect(callback);

        verify(callback, never()).run();
    }

    @Test
    void connect_withSubscribeAll_subscribesAllSymbolsOnConnected() throws Exception {
        manager.connect(manager::subscribeAll);

        fireOnConnected();

        verify(quoteManager).subscribe(eq("SYM1"), eq(listener));
        verify(quoteManager).subscribe(eq("SYM2"), eq(listener));
    }

    @Test
    void connect_withSubscribeAll_subscribesEachSymbolExactlyOnce() throws Exception {
        manager.connect(manager::subscribeAll);

        fireOnConnected();

        verify(quoteManager, times(1)).subscribe(eq("SYM1"), eq(listener));
        verify(quoteManager, times(1)).subscribe(eq("SYM2"), eq(listener));
    }

    @Test
    void connect_subscribeFailure_doesNotThrow() throws Exception {
        doThrow(new RuntimeException("subscribe error")).when(quoteManager).subscribe(any(), any());
        manager.connect(manager::subscribeAll);
        assertDoesNotThrow(this::fireOnConnected);
    }

    // --- disconnect() ---

    @Test
    void disconnect_unsubscribesAllSymbols() throws Exception {
        manager.connect(manager::subscribeAll);
        fireOnConnected();
        clearInvocations(quoteManager);

        manager.disconnect();

        verify(quoteManager).unsubscribe(eq("SYM1"), eq(listener));
        verify(quoteManager).unsubscribe(eq("SYM2"), eq(listener));
    }

    @Test
    void disconnect_callsQuoteManagerDisconnect() throws Exception {
        manager.connect(mock(Runnable.class));
        manager.disconnect();
        verify(quoteManager).disconnect();
    }

    @Test
    void disconnect_beforeConnect_doesNotThrow() {
        assertDoesNotThrow(() -> manager.disconnect());
    }

    @Test
    void disconnect_exceptionFromQuoteManager_doesNotPropagate() throws Exception {
        manager.connect(mock(Runnable.class));
        doThrow(new RuntimeException("disconnect error")).when(quoteManager).disconnect();
        assertDoesNotThrow(() -> manager.disconnect());
    }

    // --- resync() ---

    @Test
    void resync_unsubscribesThenResubscribesAllSymbols() throws Exception {
        manager.connect(manager::subscribeAll);
        fireOnConnected();
        clearInvocations(quoteManager);

        manager.resync();

        InOrder order = inOrder(quoteManager);
        order.verify(quoteManager).unsubscribe(eq("SYM1"), eq(listener));
        order.verify(quoteManager).unsubscribe(eq("SYM2"), eq(listener));
        order.verify(quoteManager).subscribe(eq("SYM1"), eq(listener));
        order.verify(quoteManager).subscribe(eq("SYM2"), eq(listener));
    }

    @Test
    void resync_subscribesEachSymbolExactlyOnce() throws Exception {
        manager.connect(manager::subscribeAll);
        fireOnConnected();
        clearInvocations(quoteManager);

        manager.resync();

        verify(quoteManager, times(1)).subscribe(eq("SYM1"), eq(listener));
        verify(quoteManager, times(1)).subscribe(eq("SYM2"), eq(listener));
    }

    // --- ConnectionListener callbacks ---

    @Test
    void onDisconnected_doesNotThrow() throws Exception {
        manager.connect(mock(Runnable.class));
        assertDoesNotThrow(() -> captureConnectionListener().onDisconnected(connectionEvent));
    }

    @Test
    void onConnecting_doesNotThrow() throws Exception {
        manager.connect(mock(Runnable.class));
        assertDoesNotThrow(() -> captureConnectionListener().onConnecting(connectionEvent));
    }

    @Test
    void onDisconnecting_doesNotThrow() throws Exception {
        manager.connect(mock(Runnable.class));
        when(connectionEvent.getStatusString()).thenReturn("closing");
        assertDoesNotThrow(() -> captureConnectionListener().onDisconnecting(connectionEvent));
    }

    @Test
    void onError_doesNotThrow() throws Exception {
        manager.connect(mock(Runnable.class));
        when(connectionEvent.getStatusString()).thenReturn("error");
        assertDoesNotThrow(() -> captureConnectionListener().onError(connectionEvent));
    }

    // --- Reconnect ---

    @Test
    void droppedSession_isLeftToSdkAutomaticRecovery_andResubscribesWhenRestored() throws Exception {
        var scheduler = new ManualScheduler();
        var reconnecting = managerWith(scheduler, () -> List.of("SYM1"));
        reconnecting.connect(reconnecting::subscribeAll);
        ConnectionListener connectionListener = captureConnectionListener();
        fire(connectionListener::onConnected);

        fire(connectionListener::onDisconnected);

        org.junit.jupiter.api.Assertions.assertTrue(scheduler.delays.isEmpty());
        verify(factory, times(1)).createQuoteManager();

        clearInvocations(quoteManager);
        fire(connectionListener::onConnecting);
        fire(connectionListener::onConnected);

        verify(quoteManager).subscribe(eq("SYM1"), eq(listener));
        org.junit.jupiter.api.Assertions.assertTrue(reconnecting.isConnected());
    }

    @Test
    void startupFailures_backOffFurtherEachTime() throws Exception {
        var scheduler = new ManualScheduler();
        var reconnecting = managerWith(scheduler, List::of);
        when(factory.createQuoteManager()).thenThrow(new RuntimeException("ICE down"));

        reconnecting.connect(mock(Runnable.class));
        scheduler.runNext();
        scheduler.runNext();

        org.junit.jupiter.api.Assertions.assertEquals(
                List.of(IceConnectionManager.BACKOFF_SECONDS[0],
                        IceConnectionManager.BACKOFF_SECONDS[1],
                        IceConnectionManager.BACKOFF_SECONDS[2]),
                scheduler.delays);
    }

    @Test
    void sdkErrorThenDisconnect_doesNotScheduleCompetingReopen() throws Exception {
        var scheduler = new ManualScheduler();
        var reconnecting = managerWith(scheduler, List::of);
        reconnecting.connect(mock(Runnable.class));
        ConnectionListener l = captureConnectionListener();

        fire(l::onError);
        fire(l::onDisconnected);

        org.junit.jupiter.api.Assertions.assertTrue(scheduler.tasks.isEmpty());
    }

    @Test
    void deliberateDisconnect_isNotReopened() throws Exception {
        var scheduler = new ManualScheduler();
        var reconnecting = managerWith(scheduler, List::of);
        reconnecting.connect(mock(Runnable.class));
        ConnectionListener l = captureConnectionListener();

        reconnecting.disconnect();
        fire(l::onDisconnected);

        org.junit.jupiter.api.Assertions.assertTrue(scheduler.tasks.isEmpty());
    }

    @Test
    void sessionRestoredByIce_isLeftOnTheSameQuoteManager() throws Exception {
        var scheduler = new ManualScheduler();
        var reconnecting = managerWith(scheduler, List::of);
        reconnecting.connect(mock(Runnable.class));
        ConnectionListener l = captureConnectionListener();
        fire(l::onDisconnected);
        fire(l::onConnected);

        org.junit.jupiter.api.Assertions.assertTrue(scheduler.tasks.isEmpty());
        verify(factory, times(1)).createQuoteManager();
    }

    @Test
    void refreshSymbols_subscribesOnlyTheNewOnes_andDropsTheGoneOnes() throws Exception {
        var lists = new java.util.ArrayDeque<List<String>>(List.of(List.of("A", "B"), List.of("B", "C")));
        var refreshing = managerWith(new ManualScheduler(), lists::poll);
        refreshing.connect(refreshing::subscribeAll);
        fire(captureConnectionListener()::onConnected);
        clearInvocations(quoteManager);

        refreshing.refreshSymbols();

        verify(quoteManager).subscribe(eq("C"), eq(listener));
        verify(quoteManager).unsubscribe(eq("A"), eq(listener));
        verify(quoteManager, never()).subscribe(eq("B"), any());
        verify(quoteManager, never()).unsubscribe(eq("B"), any());
    }

    @Test
    void refreshSymbols_whileOffline_doesNothing() {
        @SuppressWarnings("unchecked")
        java.util.function.Supplier<List<String>> source = mock(java.util.function.Supplier.class);
        managerWith(new ManualScheduler(), source).refreshSymbols();
        verify(source, never()).get();
    }

    // --- Helpers ---

    private IceConnectionManager managerWith(ManualScheduler scheduler,
            java.util.function.Supplier<List<String>> symbols) {
        return new IceConnectionManager(settings, factory, listener, symbols, scheduler.executor);
    }

    private void fire(java.util.function.Consumer<ConnectionEvent> callback) {
        when(connectionEvent.getConnectedHost()).thenReturn("icehost");
        when(connectionEvent.getStatusString()).thenReturn("status");
        callback.accept(connectionEvent);
    }

    /** Records scheduled work instead of running it, so a test can step through the backoff. */
    private static final class ManualScheduler {
        final List<Long> delays = new java.util.ArrayList<>();
        final java.util.ArrayDeque<Runnable> tasks = new java.util.ArrayDeque<>();
        final java.util.concurrent.ScheduledExecutorService executor =
                mock(java.util.concurrent.ScheduledExecutorService.class);

        ManualScheduler() {
            when(executor.schedule(any(Runnable.class), anyLong(), any())).thenAnswer(inv -> {
                tasks.add(inv.getArgument(0));
                delays.add(inv.getArgument(1));
                return mock(java.util.concurrent.ScheduledFuture.class);
            });
        }

        void runNext() {
            tasks.poll().run();
        }
    }

    private ConnectionListener captureConnectionListener() throws Exception {
        ArgumentCaptor<ConnectionListener> cap = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(quoteManager).connect(any(), cap.capture());
        return cap.getValue();
    }

    private void fireOnConnected() throws Exception {
        when(connectionEvent.getConnectedHost()).thenReturn("icehost");
        captureConnectionListener().onConnected(connectionEvent);
    }

    private static Settings makeSettings(
            String host, String user, String pass, String symbols, int port) throws Exception {
        var ctor = Settings.class.getDeclaredConstructor(
                String.class, String.class, String.class, String.class, int.class);
        ctor.setAccessible(true);
        return (Settings) ctor.newInstance(host, user, pass, symbols, port);
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertDoesNotThrow(org.junit.jupiter.api.function.Executable exec) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(exec);
    }
}