package com.braemar.ceapi.websocket;

/**
 * Builds raw array format messages to send over WebSocket.
 *
 * Format:
 *   refresh: ["refresh","SYMBOL",[[id,value],[id,value],...]]
 *   update:  ["update","SYMBOL",[[id,value],[id,value],...]]
 *   status:  ["status","MESSAGE"]
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
