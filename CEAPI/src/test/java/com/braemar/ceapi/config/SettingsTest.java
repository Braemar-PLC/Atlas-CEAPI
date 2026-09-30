package com.braemar.ceapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class SettingsTest {

    @SystemStub
    private EnvironmentVariables env;

    @TempDir
    private Path tempDir;

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
    void fromEnv_withASymbolsUrl_doesNotNeedSymbols() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols");

        Settings s = Settings.fromEnv();

        assertEquals("https://atlas.example/api/screens/symbols", s.symbolsUrl);
        assertNull(s.symbols);
    }

    @Test
    void fromEnv_usesSymbolsFileAsTheFallback() throws IOException {
        Path symbolsFile = tempDir.resolve("symbols.csv");
        Files.writeString(symbolsFile, "SYM1,SYM2");
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols")
           .set("SYMBOLS", "OLD")
           .set("SYMBOLS_FILE", symbolsFile.toString());

        Settings s = Settings.fromEnv();

        assertEquals("SYM1,SYM2", s.symbols);
    }

    @Test
    void fromEnv_failsClearlyWhenSymbolsFileCannotBeRead() {
        Path missingFile = tempDir.resolve("missing.csv");
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS_URL", "https://atlas.example/api/screens/symbols")
           .set("SYMBOLS_FILE", missingFile.toString());

        IllegalStateException ex = assertThrows(IllegalStateException.class, Settings::fromEnv);

        assertTrue(ex.getMessage().contains("Could not read SYMBOLS_FILE"));
    }

    @Test
    void fromEnv_withoutASymbolsUrl_leavesItNull() {
        env.set("ICE_HOST", "myhost")
           .set("ICE_USERNAME", "user")
           .set("ICE_PASSWORD", "pass")
           .set("SYMBOLS", "SYM1");

        assertNull(Settings.fromEnv().symbolsUrl);
    }

    @Test
    void fromEnv_throwsWithDescriptiveMessage() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class, Settings::fromEnv
        );
        assertTrue(ex.getMessage().contains("Required env var not set"));
    }
}