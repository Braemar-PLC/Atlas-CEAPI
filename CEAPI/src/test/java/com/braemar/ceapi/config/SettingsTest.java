package com.braemar.ceapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class SettingsTest {

    @SystemStub
    private EnvironmentVariables env;

    @Test
    void fromEnv_constructsCorrectly_whenAllVarsPresent() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", "SYM1,SYM2")
           .set("WS_PORT", "9002");

        Settings s = Settings.fromEnv();

        assertEquals("myhost",   s.iceHost);
        assertEquals("user",     s.iceUsername);
        assertEquals("pass",     s.icePassword);
        assertEquals("SYM1,SYM2", s.symbols);
        assertEquals(9002,        s.wsPort);
    }

    @Test
    void fromEnv_defaultsWsPortTo9001_whenNotSet() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", "SYM1");

        Settings s = Settings.fromEnv();
        assertEquals(9001, s.wsPort);
    }

    @Test
    void fromEnv_throwsIfIceHostMissing() {
        env.set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", "SYM1");

        assertThrows(IllegalStateException.class, Settings::fromEnv);
    }

    @Test
    void fromEnv_throwsIfUsernameIsMissing() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", "SYM1");

        assertThrows(IllegalStateException.class, Settings::fromEnv);
    }

    @Test
    void fromEnv_throwsIfPasswordMissing() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("SYMBOLS", "SYM1");

        assertThrows(IllegalStateException.class, Settings::fromEnv);
    }

    @Test
    void fromEnv_throwsIfSymbolsMissing() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass");

        assertThrows(IllegalStateException.class, Settings::fromEnv);
    }

    @Test
    void fromEnv_withLegacyUrl_stillRequiresSymbols() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols");

        IllegalStateException ex = assertThrows(IllegalStateException.class, Settings::fromEnv);
        assertTrue(ex.getMessage().contains("SYMBOLS"));
    }

    @Test
    void fromEnv_ignoresLegacyFileAndUrl_whenSymbolsConfigured() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols")
           .set("SYMBOLS", "SYM1,SYM2")
           .set("SYMBOLS_FILE", "missing.csv");

        Settings s = Settings.fromEnv();

        assertEquals("SYM1,SYM2", s.symbols);
        assertEquals(java.util.List.of("SYM1", "SYM2"), new SymbolListLoader(s).get());
    }

    @Test
    void fromEnv_withLegacyFile_stillRequiresSymbols() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols")
           .set("SYMBOLS_FILE", "missing.csv");

        IllegalStateException ex = assertThrows(IllegalStateException.class, Settings::fromEnv);

        assertTrue(ex.getMessage().contains("Required env var not set: SYMBOLS"));
    }

    @Test
    void fromEnv_rejectsBlankSymbolList() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", " , , \"\" ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, Settings::fromEnv);
        assertTrue(ex.getMessage().contains("at least one non-blank symbol"));
    }

    @Test
    void fromEnv_preserves203ConfiguredSymbols_withoutExpansion() {
        String symbols = java.util.stream.IntStream.rangeClosed(1, 203)
                .mapToObj(i -> "SYM" + i)
                .collect(java.util.stream.Collectors.joining(","));
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", symbols)
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols")
           .set("SYMBOLS_FILE", "missing.csv");

        Settings settings = Settings.fromEnv();
        assertEquals(symbols, settings.symbols);
        assertEquals(SymbolListLoader.parse(symbols), new SymbolListLoader(settings).get());
        assertEquals(203, new SymbolListLoader(settings).get().size());
    }

    @Test
    void fromEnv_throwsWithDescriptiveMessage() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class, Settings::fromEnv
        );
        assertTrue(ex.getMessage().contains("Required env var not set"));
    }
}