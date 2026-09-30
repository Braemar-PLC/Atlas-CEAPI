package com.braemar.ceapi.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Settings {

    public final String iceHost;
    public final String iceUsername;
    public final String icePassword;
    /** Comma-separated fallback list; may be null when {@link #symbolsUrl} is set. */
    public final String symbols;
    /** Where the Web API publishes the symbols its screens need (GET /api/screens/symbols); may be null. */
    public final String symbolsUrl;
    public final int wsPort;

    private Settings(String iceHost, String iceUsername, String icePassword, String symbols, int wsPort) {
        this(iceHost, iceUsername, icePassword, symbols, null, wsPort);
    }

    private Settings(String iceHost, String iceUsername, String icePassword, String symbols, String symbolsUrl,
            int wsPort) {
        this.iceHost = iceHost;
        this.iceUsername = iceUsername;
        this.icePassword = icePassword;
        this.symbols = symbols;
        this.symbolsUrl = symbolsUrl;
        this.wsPort = wsPort;
    }

    public static Settings fromEnv() {
        String iceHost = requireEnv("ICE_HOST");
        String symbolsUrl = optionalEnv("SYMBOLS_URL");
        String symbols = loadSymbolsFallback(symbolsUrl);
        String username = requireEnv("ICE_USERNAME");
        String password = requireEnv("ICE_PASSWORD");
        String portStr = optionalEnv("WS_PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 9001;
        return new Settings(iceHost, username, password, symbols, symbolsUrl, port);
    }

    private static String loadSymbolsFallback(String symbolsUrl) {
        String symbolsFile = optionalEnv("SYMBOLS_FILE");
        if (symbolsFile != null) {
            try {
                return Files.readString(Path.of(symbolsFile));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read SYMBOLS_FILE: " + symbolsFile, e);
            }
        }

        // With a URL the list comes from the Web API; SYMBOLS is then only the fallback for when it cannot answer.
        return symbolsUrl != null ? optionalEnv("SYMBOLS") : requireEnv("SYMBOLS");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(name);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Required env var not set: " + name);
        }
        return value;
    }

    private static String optionalEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty())
            value = System.getProperty(name);
        if (value == null || value.isEmpty())
            return null;
        return value;
    }
}