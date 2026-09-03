package com.igrium.craftui.api.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple event designed for UI usage.
 */
public interface UIEvent<T> {
    T invoker();
    void addListener(T listener);
    void removeListener(T listener);

    /**
     * Create a new UI event backed by a collection and invoker factory.
     * 
     * @param <T>            Event type
     * @param invokerFactory Invoker factory
     * @return The event
     */
    static <T> UIEvent<T> collectionBacked(Function<Collection<? extends T>, T> invokerFactory) {
        return new CollectionBackedUIEvent<>(invokerFactory);
    }

    /**
     * Create a new UI event with consumers as listeners.
     * 
     * @param <T> Event argument type
     * @return The event
     */
    static <T> UIEvent<Consumer<T>> ofConsumer() {
        return new CollectionBackedUIEvent<>(listeners -> val -> {
            for (var l : listeners) {
                l.accept(val);
            }
        });
    }

    /**
     * Create a new UI event that will not pass any parameters to its listeners.
     * 
     * @return The event
     */
    static UIEvent<Runnable> ofRunnable() {
        return new CollectionBackedUIEvent<>(listeners -> () -> {
            for (var l : listeners) {
                l.run();
            }
        });
    }
}

class CollectionBackedUIEvent<T> implements UIEvent<T> {

    private final Function<Collection<? extends T>, T> invokerFactory;

    private @NotNull T invoker;

    public CollectionBackedUIEvent(Function<Collection<? extends T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        invoker = invokerFactory.apply(Collections.emptyList());
    }

    private final Set<T> listeners = new HashSet<>();
    private final Set<T> unmodifiableListeners = Collections.unmodifiableSet(listeners);

    @Override
    public void addListener(T listener) {
        listeners.add(listener);
        invoker = invokerFactory.apply(unmodifiableListeners);
    }

    @Override
    public void removeListener(T listener) {
        listeners.remove(listener);
        invoker = invokerFactory.apply(unmodifiableListeners);
    }
    
    @Override
    public T invoker() {
        return invoker;
    }
}
