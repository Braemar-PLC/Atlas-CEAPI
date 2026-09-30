package com.braemar.ceapi.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SymbolListLoaderTest {

    private static final String URL = "https://atlas.example/api/screens/symbols";

    @Test
    void parse_trimsDropsBlanksQuotesAndRepeats() {
        assertEquals(List.of("TFM 26X-ICN", "B"), SymbolListLoader.parse(" TFM 26X-ICN ,\"B\",, B ,"));
    }

    @Test
    void parse_null_isEmpty() {
        assertEquals(List.of(), SymbolListLoader.parse(null));
    }

    @Test
    void withAUrl_usesTheWebApisList() {
        var loader = new SymbolListLoader(URL, "FALLBACK", url -> "A,B", Duration.ZERO);
        assertEquals(List.of("A", "B"), loader.get());
    }

    @Test
    void withoutAUrl_usesSymbols() {
        var loader = new SymbolListLoader(null, "X,Y", url -> { throw new AssertionError("not called"); }, Duration.ZERO);
        assertEquals(List.of("X", "Y"), loader.get());
    }

    @Test
    void aFailedAttempt_isRetried() {
        var calls = new AtomicInteger();
        var loader = new SymbolListLoader(URL, "FALLBACK", url -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("HTTP 503");
            }
            return "A";
        }, Duration.ZERO);

        assertEquals(List.of("A"), loader.get());
        assertEquals(2, calls.get());
    }

    @Test
    void whenTheWebApiNeverAnswers_fallsBackToSymbols_afterEveryAttempt() {
        var calls = new AtomicInteger();
        var loader = new SymbolListLoader(URL, "X", url -> {
            calls.incrementAndGet();
            throw new IllegalStateException("down");
        }, Duration.ZERO);

        assertEquals(List.of("X"), loader.get());
        assertEquals(SymbolListLoader.ATTEMPTS, calls.get());
    }

    @Test
    void anEmptyAnswer_isNotTrusted() {
        var loader = new SymbolListLoader(URL, "X", url -> " ", Duration.ZERO);
        assertEquals(List.of("X"), loader.get());
    }

    @Test
    void whenTheWebApiStopsAnswering_theLastListItGaveIsKept() {
        var up = new boolean[] {true};
        var loader = new SymbolListLoader(URL, "X", url -> {
            if (!up[0]) {
                throw new IllegalStateException("down");
            }
            return "A,B";
        }, Duration.ZERO);
        loader.get();
        up[0] = false;

        assertEquals(List.of("A", "B"), loader.get());
    }

    @Test
    void nothingAnywhere_isEmpty() {
        var loader = new SymbolListLoader(URL, null, url -> { throw new IllegalStateException("down"); }, Duration.ZERO);
        assertEquals(List.of(), loader.get());
    }
}
