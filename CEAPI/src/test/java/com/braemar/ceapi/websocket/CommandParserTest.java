package com.braemar.ceapi.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    @Test
    void parse_resync_returnsResync() {
        assertEquals(CommandParser.Command.RESYNC, CommandParser.parse("resync"));
    }

    @Test
    void parse_resync_isCaseInsensitive() {
        assertEquals(CommandParser.Command.RESYNC, CommandParser.parse("RESYNC"));
        assertEquals(CommandParser.Command.RESYNC, CommandParser.parse("Resync"));
    }

    @Test
    void parse_resync_trimsWhitespace() {
        assertEquals(CommandParser.Command.RESYNC, CommandParser.parse("  resync  "));
    }

    @Test
    void parse_unknownCommand_returnsUnknown() {
        assertEquals(CommandParser.Command.UNKNOWN, CommandParser.parse("blah"));
    }

    @Test
    void parse_null_returnsUnknown() {
        assertEquals(CommandParser.Command.UNKNOWN, CommandParser.parse(null));
    }

    @Test
    void parse_empty_returnsUnknown() {
        assertEquals(CommandParser.Command.UNKNOWN, CommandParser.parse(""));
    }
}
