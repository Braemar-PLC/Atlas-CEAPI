package com.braemar.ceapi.utility;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class Observable<T> {
    private final List<Consumer<T>> observers = new CopyOnWriteArrayList<>();
    public void subscribe(Consumer<T> h) { observers.add(h); }
    public void unsubscribe(Consumer<T> h) { observers.remove(h); }
    public void raise(T e) { observers.forEach(h -> h.accept(e)); }
}
