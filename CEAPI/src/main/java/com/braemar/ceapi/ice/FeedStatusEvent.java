package com.braemar.ceapi.ice;

import java.time.Instant;

public record FeedStatusEvent(
        FeedState state,
        int generation,
        Instant timestamp,
        String detail,
        int subscribedSymbols) {
}
