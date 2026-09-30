package com.braemar.ceapi.websocket;

import com.braemar.ceapi.ice.FeedState;
import com.braemar.ceapi.ice.FeedStatusEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageBuilderTest {

    @Test
    void refresh_buildsCorrectFormat() {
        short[] ids = {2003, 2005};
        String[] values = {"51.11", "51.14"};

        String result = MessageBuilder.refresh("TFM 26J-ICN", ids, values);

        assertEquals("[\"refresh\",\"TFM 26J-ICN\",[[2003,\"51.11\"],[2005,\"51.14\"]]]", result);
    }

    @Test
    void update_buildsCorrectFormat() {
        short[] ids = {2003};
        String[] values = {"51.12"};

        String result = MessageBuilder.update("TFM 26J-ICN", ids, values);

        assertEquals("[\"update\",\"TFM 26J-ICN\",[[2003,\"51.12\"]]]", result);
    }

    @Test
    void status_buildsCorrectFormat() {
        String result = MessageBuilder.status("Connected");

        assertEquals("[\"status\",\"Connected\"]", result);
    }

    @Test
    void status_buildsStructuredHealthMessage() {
        String result = MessageBuilder.status(new FeedStatusEvent(
                FeedState.LIVE,
                7,
                Instant.parse("2026-09-30T13:00:00Z"),
                "Connected",
                3866));

        assertTrue(result.contains("\"state\":\"LIVE\""));
        assertTrue(result.contains("\"generation\":7"));
        assertTrue(result.contains("\"subscribedSymbols\":3866"));
    }

    @Test
    void refresh_escapesQuotesInSymbol() {
        short[] ids = {2003};
        String[] values = {"51.11"};

        String result = MessageBuilder.refresh("SYM\"BAD", ids, values);

        assertTrue(result.contains("SYM\\\"BAD"));
    }

    @Test
    void refresh_escapesQuotesInValue() {
        short[] ids = {2003};
        String[] values = {"bad\"value"};

        String result = MessageBuilder.refresh("SYM", ids, values);

        assertTrue(result.contains("bad\\\"value"));
    }

    @Test
    void refresh_handlesEmptyFields() {
        short[] ids = {};
        String[] values = {};

        String result = MessageBuilder.refresh("TFM", ids, values);

        assertEquals("[\"refresh\",\"TFM\",[]]", result);
    }

    @Test
    void refresh_handlesNullValue() {
        short[] ids = {2003};
        String[] values = {null};

        String result = MessageBuilder.refresh("TFM", ids, values);

        assertEquals("[\"refresh\",\"TFM\",[[2003,\"\"]]]", result);
    }
}
