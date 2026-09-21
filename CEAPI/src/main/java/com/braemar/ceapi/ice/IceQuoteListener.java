package com.braemar.ceapi.ice;

import com.esignal.jstandard.beans.quote.Quote;
import com.esignal.jstandard.beans.quote.Quote.FieldItem;
import com.esignal.jstandard.event.QuoteEvent;
import com.esignal.jstandard.event.QuoteListener;
import com.esignal.jstandard.event.QuoteRequestListener;
import com.esignal.jstandard.event.StatusEvent;
import com.esignal.jstandard.event.SymbolEvent;
import com.braemar.ceapi.utility.Observable;
import com.braemar.ceapi.websocket.MessageBuilder;
import com.braemar.ceapi.websocket.MessageType;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dumb relay — receives ICE callbacks, converts to QuoteReceivedEvent and
 * raises it.
 * onResponse = initial snapshot from ICE, forwarded as "refresh".
 * onUpdate = delta from ICE, forwarded as "update".
 */
public class IceQuoteListener implements QuoteRequestListener, QuoteListener {
    private static final Logger log = Logger.getLogger(IceQuoteListener.class.getName());
    public final Observable<QuoteReceivedEvent> quoteEventEmitter;
    public final Observable<StatusEvent> statusEventEmitter;
    public final Observable<SymbolEvent> symbolEventEmitter;

    private static final DateTimeFormatter UTC_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    public IceQuoteListener(Observable<QuoteReceivedEvent> quoteEventEmitter,
            Observable<StatusEvent> statusEventEmitter, Observable<SymbolEvent> symbolEventEmitter) {
        this.quoteEventEmitter = quoteEventEmitter;
        this.statusEventEmitter = statusEventEmitter;
        this.symbolEventEmitter = symbolEventEmitter;
    }

    @Override
    public void onResponse(QuoteEvent event) {
        forward(event.getQuote(), MessageType.REFRESH);
    }

    @Override
    public void onUpdate(QuoteEvent event) {
        forward(event.getQuote(), MessageType.UPDATE);
    }

    @Override
    public void onError(StatusEvent event) {
        log.warning("ICE status error");
        statusEventEmitter.raise(event);
    }

    @Override
    public void onError(SymbolEvent event) {
        log.warning("ICE symbol error: " + event.getSymbol() + " status: " + event.getStatus());
        symbolEventEmitter.raise(event);
    }

    @Override
    public void onAdded(SymbolEvent event) {
        symbolEventEmitter.raise(event);
    }

    @Override
    public void onDeleted(SymbolEvent event) {
        symbolEventEmitter.raise(event);
    }

    @Override
    public void onComplete(SymbolEvent event) {
        symbolEventEmitter.raise(event);
    }

    @Override
    public void onRequested(SymbolEvent event) {
        symbolEventEmitter.raise(event);
    }

    private void forward(Quote quote, MessageType messageType) {
        String symbol = quote.getSymbol();

        List<Short> ids = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (FieldItem item : quote) {
            if (quote.isFieldItemNull(item.getId()))
                continue;
            ids.add(item.getId());
            values.add(safeFieldValue(item));
        }

        if (ids.isEmpty())
            return;

        short[] fieldIds = new short[ids.size()];
        String[] fieldValues = new String[values.size()];
        for (int i = 0; i < ids.size(); i++) {
            fieldIds[i] = ids.get(i);
            fieldValues[i] = values.get(i);
        }

        String message = messageType == MessageType.REFRESH
                ? MessageBuilder.refresh(symbol, fieldIds, fieldValues)
                : MessageBuilder.update(symbol, fieldIds, fieldValues);

        log.fine(message);

        QuoteReceivedEvent e = new QuoteReceivedEvent(symbol, fieldIds, fieldValues, messageType);
        this.quoteEventEmitter.raise(e);

    }

    private String safeFieldValue(FieldItem item) {
        try {
            switch (item.getFormat()) {
                case BYTE:
                    return String.valueOf(item.getValueAsByte());
                case BYTEARRAY:
                case STRINGARRAY:
                    return Arrays.toString(item.getValueAsByteArray());
                case CIDDATA:
                    return item.getValueAsCidData().toString();
                case CIDFIELDDATA:
                    return String.valueOf(item.getValueAsCidFieldData().getValue());
                case CIDPRICEFIELDDATA:
                    return String.valueOf(item.getValueAsCidPriceFieldData().getValue());
                case DATE: {
                    long epochMs = item.getValueAsInteger(false) * 1000L;
                    return UTC_FMT.format(Instant.ofEpochMilli(epochMs));
                }
                case DOUBLE:
                    return String.valueOf(item.getValueAsDouble());
                case INT32:
                    return String.valueOf(item.getValueAsInteger(false));
                case SHORT:
                    return String.valueOf(item.getValueAsShort(true));
                case STRING:
                    return item.getValueAsString();
                case UINT32:
                    return String.valueOf(item.getValueAsInteger(false));
                case UINT64:
                    BigInteger bigInt = item.getValueAsBigInteger(false);
                    return bigInt != null ? bigInt.toString() : "";
                default:
                    Object raw = item.getValue();
                    return raw != null ? raw.toString() : "";
            }
        } catch (Exception e) {
            log.warning("Could not read field id=" + item.getId()
                    + " format=" + item.getFormat() + ": " + e.getMessage());
            return "";
        }
    }

    public void reset() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reset'");
    }
}