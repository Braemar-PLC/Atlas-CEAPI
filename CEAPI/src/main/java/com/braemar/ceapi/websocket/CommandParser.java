package com.braemar.ceapi.websocket;

/**
 * Parses raw command strings received from the Web API over WebSocket.
 * Commands are simple strings — no JSON parsing needed.
 *
 * Supported commands:
 *   resync   — resubscribe all symbols and send fresh REFRESHes
 */
public class CommandParser {

    public enum Command {
        RESYNC,
        UNKNOWN
    }

    public static Command parse(String raw) {
        if (raw == null) return Command.UNKNOWN;
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.equals("resync")) return Command.RESYNC;
        return Command.UNKNOWN;
    }
}
