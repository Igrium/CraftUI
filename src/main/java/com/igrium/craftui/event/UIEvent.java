package com.igrium.craftui.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An event designed for UI usage.
 */
public interface UIEvent<T> {
    public T invoker();
    public void addListener(T listener);
    public void removeListener(Object listener);

    /**
     * Create a new UI event backed by a collection and invoker factory.
     * 
     * @param <T>            Event type
     * @param invokerFactory Invoker factory
     * @return The event
     */
    public static <T> UIEvent<T> collectionBacked(Function<Collection<? extends T>, T> invokerFactory) {
        return new CollectionBackedUIEvent<>(invokerFactory);
    }

    /**
     * Create a new UI event with consumers as listeners.
     * 
     * @param <T> Event argument type
     * @return The event
     */
    public static <T> UIEvent<Consumer<T>> ofConsumer() {
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
    public static UIEvent<Runnable> ofRunnable() {
        return new CollectionBackedUIEvent<>(listeners -> () -> {
            for (var l : listeners) {
                l.run();
            }
        });
    }
}

class CollectionBackedUIEvent<T> implements UIEvent<T> {

    private final Function<Collection<? extends T>, T> invokerFactory;

    public CollectionBackedUIEvent(Function<Collection<? extends T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
    }

    private final Set<T> listeners = new HashSet<>();
    private final Set<T> unmodifiableListeners = Collections.unmodifiableSet(listeners);

    @Override
    public void addListener(T listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Object listener) {
        listeners.remove(listener);
    }
    
    @Override
    public T invoker() {
        // The factory will never see garbage-collected listeners
        return invokerFactory.apply(unmodifiableListeners);
    }
}
