package com.braemar.ceapi.websocket;

import com.braemar.ceapi.ice.FeedStatusEvent;

/**
 * Builds raw array format messages to send over WebSocket.
 *
 * Format:
 *   refresh: ["refresh","SYMBOL",[[id,value],[id,value],...]]
 *   update:  ["update","SYMBOL",[[id,value],[id,value],...]]
 *   status:  ["status",{"state":"LIVE","generation":1,...}]
 */
public class MessageBuilder {

    public static String refresh(String symbol, short[] fieldIds, String[] fieldValues) {
        return buildMessage("refresh", symbol, fieldIds, fieldValues);
    }

    public static String update(String symbol, short[] fieldIds, String[] fieldValues) {
        return buildMessage("update", symbol, fieldIds, fieldValues);
    }

    public static String status(String message) {
        return "[\"status\",\"" + escape(message) + "\"]";
    }

    public static String status(FeedStatusEvent event) {
        return "[\"status\",{"
                + "\"state\":\"" + event.state() + "\","
                + "\"generation\":" + event.generation() + ","
                + "\"timestamp\":\"" + event.timestamp() + "\","
                + "\"detail\":\"" + escape(event.detail()) + "\","
                + "\"subscribedSymbols\":" + event.subscribedSymbols()
                + "}]";
    }

    private static String buildMessage(String type, String symbol, short[] fieldIds, String[] fieldValues) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\"").append(type).append("\",\"").append(escape(symbol)).append("\",[");
        for (int i = 0; i < fieldIds.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("[").append(fieldIds[i]).append(",\"").append(escape(fieldValues[i])).append("\"]");
        }
        sb.append("]]");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
