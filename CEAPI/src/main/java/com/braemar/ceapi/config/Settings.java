package com.braemar.ceapi.config;

public class Settings {

    public final String iceHost;
    public final String iceUsername;
    public final String icePassword;
    public final String symbols;
    public final int wsPort;

    private Settings(String iceHost, String iceUsername, String icePassword, String symbols, int wsPort) {
        this.iceHost = iceHost;
        this.iceUsername = iceUsername;
        this.icePassword = icePassword;
        this.symbols = symbols;
        this.wsPort = wsPort;
    }

    public static Settings fromEnv() {
        String iceHost = requireEnv("ICE_HOST");
        String symbols = requireEnv("SYMBOLS");
        String username = requireEnv("ICE_USERNAME");
        String password = requireEnv("ICE_PASSWORD");
        String portStr = optionalEnv("WS_PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 9001;
        return new Settings(iceHost, username, password, symbols, port);
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
