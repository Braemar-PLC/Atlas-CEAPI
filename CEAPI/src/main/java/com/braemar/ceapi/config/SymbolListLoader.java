package com.braemar.ceapi.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * The symbols to subscribe to. The Web API works them out from the desk screens (they roll as contracts expire
 * and the option chains follow the futures), so they are fetched from it on every ICE connect. Tried in order:
 * the URL (a few attempts, as the Web API may still be starting), the last list it gave, then the SYMBOLS setting.
 */
public class SymbolListLoader implements Supplier<List<String>> {

    /** Reads a URL's body; an exception or a non-200 answer counts as a failed attempt. */
    @FunctionalInterface
    public interface Fetcher {
        String get(String url) throws Exception;
    }

    private static final Logger logger = Logger.getLogger(SymbolListLoader.class.getName());
    static final int ATTEMPTS = 3;

    private final String url;
    private final String fallback;
    private final Fetcher fetcher;
    private final Duration retryDelay;
    private volatile List<String> lastGood;

    public SymbolListLoader(Settings settings) {
        this(settings.symbolsUrl, settings.symbols, SymbolListLoader::httpGet, Duration.ofSeconds(3));
    }

    public SymbolListLoader(String url, String fallback, Fetcher fetcher, Duration retryDelay) {
        this.url = url;
        this.fallback = fallback;
        this.fetcher = fetcher;
        this.retryDelay = retryDelay;
    }

    @Override
    public List<String> get() {
        if (url != null) {
            for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
                try {
                    List<String> symbols = parse(fetcher.get(url));
                    if (!symbols.isEmpty()) {
                        logger.info("Fetched " + symbols.size() + " symbols from " + url);
                        lastGood = symbols;
                        return symbols;
                    }
                    logger.warning("The symbol list from " + url + " was empty (attempt " + attempt + ")");
                } catch (Exception e) {
                    logger.warning("Could not fetch symbols from " + url + " (attempt " + attempt + "): " + e);
                }
                if (attempt < ATTEMPTS && !sleep()) {
                    break;
                }
            }
            if (lastGood != null) {
                logger.warning("Using the last symbol list fetched (" + lastGood.size() + " symbols)");
                return lastGood;
            }
        }
        List<String> symbols = parse(fallback);
        if (symbols.isEmpty()) {
            logger.severe("No symbols to subscribe to: the Web API did not answer and SYMBOLS is not set");
        } else if (url != null) {
            logger.warning("Using the " + symbols.size() + " symbols in SYMBOLS instead");
        }
        return symbols;
    }

    /** Comma-separated, as the SYMBOLS setting and GET /api/screens/symbols both are; blanks and repeats dropped. */
    public static List<String> parse(String csv) {
        if (csv == null) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(s -> s.replace("\"", "").trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private boolean sleep() {
        try {
            Thread.sleep(retryDelay.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static String httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }
}
