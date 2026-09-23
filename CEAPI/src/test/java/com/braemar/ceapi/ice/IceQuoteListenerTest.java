package com.braemar.ceapi.ice;

import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.MessageType;
import com.esignal.jstandard.beans.quote.CidFieldData;
import com.esignal.jstandard.beans.quote.FIELDFORMAT;
import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.*;

class IceQuoteListenerTest {

    private final List<QuoteReceivedEvent> received = new ArrayList<>();
    private final List<StatusEvent> statusEvents = new ArrayList<>();
    private final List<SymbolEvent> symbolEvents = new ArrayList<>();

    private Observable<QuoteReceivedEvent> quoteEmitter;
    private Observable<StatusEvent> statusEmitter;
    private Observable<SymbolEvent> symbolEmitter;

    @Mock
    private QuoteEvent quoteEvent;
    @Mock
    private Quote quote;
    @Mock
    private FieldItem fieldItem;

    private IceQuoteListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        received.clear();
        statusEvents.clear();
        symbolEvents.clear();

        quoteEmitter = new Observable<>();
        statusEmitter = new Observable<>();
        symbolEmitter = new Observable<>();

        quoteEmitter.subscribe(received::add);
        statusEmitter.subscribe(statusEvents::add);
        symbolEmitter.subscribe(symbolEvents::add);

        listener = new IceQuoteListener(quoteEmitter, statusEmitter, symbolEmitter);
        when(quoteEvent.getQuote()).thenReturn(quote);
        when(quote.getSymbol()).thenReturn("TFM 26J-ICN");
    }

    private void stubField(FIELDFORMAT format) {
        when(quote.iterator()).thenReturn(List.of(fieldItem).iterator());
        when(quote.isFieldItemNull(anyShort())).thenReturn(false);
        when(fieldItem.getId()).thenReturn((short) 2003);
        when(fieldItem.getFormat()).thenReturn(format);
    }

    // --- Routing ---

    @Test
    void onResponse_raisesRefreshEvent() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onResponse(quoteEvent);
        assertEquals(1, received.size());
        assertEquals(MessageType.REFRESH, received.get(0).type);
    }

    @Test
    void onUpdate_raisesUpdateEvent() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onUpdate(quoteEvent);
        assertEquals(1, received.size());
        assertEquals(MessageType.UPDATE, received.get(0).type);
    }

    @Test
    void onUpdate_calledTwice_bothAreUpdates() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onUpdate(quoteEvent);
        when(quote.iterator()).thenReturn(List.of(fieldItem).iterator());
        listener.onUpdate(quoteEvent);
        assertEquals(2, received.size());
        assertEquals(MessageType.UPDATE, received.get(0).type);
        assertEquals(MessageType.UPDATE, received.get(1).type);
    }

    @Test
    void onResponse_calledTwice_bothAreRefreshes() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onResponse(quoteEvent);
        when(quote.iterator()).thenReturn(List.of(fieldItem).iterator());
        listener.onResponse(quoteEvent);
        assertEquals(2, received.size());
        assertEquals(MessageType.REFRESH, received.get(0).type);
        assertEquals(MessageType.REFRESH, received.get(1).type);
    }

    // --- Filtering ---

    @Test
    void emptyQuote_raisesNoEvent() {
        when(quote.iterator()).thenReturn(Collections.emptyIterator());
        listener.onUpdate(quoteEvent);
        assertTrue(received.isEmpty());
    }

    @Test
    void nullField_isSkipped_andIfOnlyField_raisesNoEvent() {
        when(quote.iterator()).thenReturn(List.of(fieldItem).iterator());
        when(quote.isFieldItemNull(anyShort())).thenReturn(true);
        listener.onUpdate(quoteEvent);
        assertTrue(received.isEmpty());
    }

    // --- Event content ---

    @Test
    void event_containsCorrectSymbol() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onUpdate(quoteEvent);
        assertEquals("TFM 26J-ICN", received.get(0).symbol);
    }

    @Test
    void event_containsFieldIdAndValue() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onUpdate(quoteEvent);
        assertEquals(2003, received.get(0).fieldIds[0]);
        assertEquals("51.11", received.get(0).fieldValues[0]);
    }

    // --- Field format serialisation ---

    @Test
    void fieldFormat_double_serialisesCorrectly() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenReturn(51.11);
        listener.onUpdate(quoteEvent);
        assertEquals("51.11", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_string_serialisesCorrectly() {
        stubField(FIELDFORMAT.STRING);
        when(fieldItem.getValueAsString()).thenReturn("hello");
        listener.onUpdate(quoteEvent);
        assertEquals("hello", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_int32_serialisesCorrectly() {
        stubField(FIELDFORMAT.INT32);
        when(fieldItem.getValueAsInteger(false)).thenReturn(42);
        listener.onUpdate(quoteEvent);
        assertEquals("42", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_short_serialisesCorrectly() {
        stubField(FIELDFORMAT.SHORT);
        when(fieldItem.getValueAsShort(true)).thenReturn((short) 7);
        listener.onUpdate(quoteEvent);
        assertEquals("7", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_uint32_serialisesCorrectly() {
        stubField(FIELDFORMAT.UINT32);
        when(fieldItem.getValueAsInteger(false)).thenReturn(100);
        listener.onUpdate(quoteEvent);
        assertEquals("100", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_uint64_serialisesCorrectly() {
        stubField(FIELDFORMAT.UINT64);
        when(fieldItem.getValueAsBigInteger(false)).thenReturn(BigInteger.valueOf(9999999999L));
        listener.onUpdate(quoteEvent);
        assertEquals("9999999999", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_uint64_nullValue_serialisesAsEmpty() {
        stubField(FIELDFORMAT.UINT64);
        when(fieldItem.getValueAsBigInteger(false)).thenReturn(null);
        listener.onUpdate(quoteEvent);
        assertEquals("", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_byte_serialisesCorrectly() {
        stubField(FIELDFORMAT.BYTE);
        when(fieldItem.getValueAsByte()).thenReturn((byte) 5);
        listener.onUpdate(quoteEvent);
        assertEquals("5", received.get(0).fieldValues[0]);
    }

    @Test
    void fieldFormat_date_serialisesAsIso8601() {
        stubField(FIELDFORMAT.DATE);
        when(fieldItem.getValueAsInteger(false)).thenReturn(0);
        listener.onUpdate(quoteEvent);
        assertEquals("1970-01-01T00:00:00Z", received.get(0).fieldValues[0]);
    }

    // --- Composite bid/ask: CIDFIELDDATA carries price and size in one item ---

    private CidFieldData stubCidField(short id, double price, double size) {
        stubField(FIELDFORMAT.CIDFIELDDATA);
        when(fieldItem.getId()).thenReturn(id);
        CidFieldData cid = mock(CidFieldData.class);
        when(cid.getValue()).thenReturn(price);
        when(cid.getSize()).thenReturn(size);
        when(fieldItem.getValueAsCidFieldData()).thenReturn(cid);
        return cid;
    }

    @Test
    void cidFieldData_bid_emitsPriceThenBidSize() {
        stubCidField((short) FieldItem.LRT_TYPE_BID, 49.26, 5.0);
        listener.onUpdate(quoteEvent);
        assertArrayEquals(new short[] { 20, 30 }, received.get(0).fieldIds);
        assertArrayEquals(new String[] { "49.26", "5.0" }, received.get(0).fieldValues);
    }

    @Test
    void cidFieldData_ask_emitsPriceThenAskSize() {
        stubCidField((short) FieldItem.LRT_TYPE_ASK, 49.745, 10.0);
        listener.onUpdate(quoteEvent);
        assertArrayEquals(new short[] { 21, 31 }, received.get(0).fieldIds);
        assertArrayEquals(new String[] { "49.745", "10.0" }, received.get(0).fieldValues);
    }

    @Test
    void cidFieldData_otherField_emitsPriceOnly() {
        stubCidField((short) FieldItem.LRT_TYPE_LAST, 49.5, 7.0);
        listener.onUpdate(quoteEvent);
        assertArrayEquals(new short[] { 19 }, received.get(0).fieldIds);
        assertArrayEquals(new String[] { "49.5" }, received.get(0).fieldValues);
    }

    @Test
    void cidFieldData_zeroSize_isStillEmitted() {
        stubCidField((short) FieldItem.LRT_TYPE_BID, 49.26, 0.0);
        listener.onUpdate(quoteEvent);
        assertArrayEquals(new short[] { 20, 30 }, received.get(0).fieldIds);
        assertArrayEquals(new String[] { "49.26", "0.0" }, received.get(0).fieldValues);
    }

    @Test
    void cidFieldData_unreadableSize_emitsPriceWithoutSize() {
        CidFieldData cid = stubCidField((short) FieldItem.LRT_TYPE_BID, 49.26, 5.0);
        when(cid.getSize()).thenThrow(new RuntimeException("ICE error"));
        listener.onUpdate(quoteEvent);
        assertArrayEquals(new short[] { 20 }, received.get(0).fieldIds);
        assertArrayEquals(new String[] { "49.26" }, received.get(0).fieldValues);
    }

    // --- Error resilience ---

    @Test
    void brokenField_serialisesAsEmptyString_andEventStillRaised() {
        stubField(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenThrow(new RuntimeException("ICE error"));
        listener.onUpdate(quoteEvent);
        assertEquals(1, received.size());
        assertEquals("", received.get(0).fieldValues[0]);
    }

    @Test
    void brokenField_doesNotPreventOtherFieldsBeingIncluded() {
        FieldItem goodItem = mock(FieldItem.class);
        when(quote.iterator()).thenReturn(List.of(fieldItem, goodItem).iterator());
        when(quote.isFieldItemNull(anyShort())).thenReturn(false);

        when(fieldItem.getId()).thenReturn((short) 2003);
        when(fieldItem.getFormat()).thenReturn(FIELDFORMAT.DOUBLE);
        when(fieldItem.getValueAsDouble()).thenThrow(new RuntimeException("ICE error"));

        when(goodItem.getId()).thenReturn((short) 2005);
        when(goodItem.getFormat()).thenReturn(FIELDFORMAT.DOUBLE);
        when(goodItem.getValueAsDouble()).thenReturn(51.14);

        listener.onUpdate(quoteEvent);

        assertEquals(1, received.size());
        assertEquals(2005, received.get(0).fieldIds[1]);
        assertEquals("51.14", received.get(0).fieldValues[1]);
    }

    // --- Error event propagation ---

    @Test
    void onStatusError_raisesStatusEvent() {
        StatusEvent event = mock(StatusEvent.class);
        listener.onError(event);
        assertEquals(1, statusEvents.size());
        assertSame(event, statusEvents.get(0));
    }

    @Test
    void onSymbolError_raisesSymbolEvent() {
        SymbolEvent event = mock(SymbolEvent.class);
        when(event.getSymbol()).thenReturn("TFM");
        listener.onError(event);
        assertEquals(1, symbolEvents.size());
        assertSame(event, symbolEvents.get(0));
    }

    @Test
    void onAdded_raisesSymbolEvent() {
        SymbolEvent event = mock(SymbolEvent.class);
        listener.onAdded(event);
        assertEquals(1, symbolEvents.size());
    }

    @Test
    void onDeleted_raisesSymbolEvent() {
        SymbolEvent event = mock(SymbolEvent.class);
        listener.onDeleted(event);
        assertEquals(1, symbolEvents.size());
    }
}