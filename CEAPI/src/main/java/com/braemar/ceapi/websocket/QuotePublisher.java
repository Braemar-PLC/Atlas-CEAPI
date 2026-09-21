package com.braemar.ceapi.websocket;

/**
 * Transport abstraction for publishing quote messages.
 */
public interface QuotePublisher {
    void publish(String message);
}
