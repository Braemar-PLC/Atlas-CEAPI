package com.braemar.ceapi.ice;

import java.time.Instant;

import com.braemar.ceapi.websocket.MessageType;

public class QuoteReceivedEvent {
    public final String symbol;
    public final short[] fieldIds;
    public final String[] fieldValues;
    public final MessageType type;
    public final Instant timestamp = Instant.now();

    public QuoteReceivedEvent(String symbol, short[] fieldIds, String[] fieldValues, MessageType type) {
        this.symbol = symbol;
        this.fieldIds = fieldIds;
        this.fieldValues = fieldValues;
        this.type = type;
    }
}