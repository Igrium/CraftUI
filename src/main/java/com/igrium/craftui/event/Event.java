package com.igrium.craftui.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public interface Event<T> {
    public T invoker();
    public void addListener(T listener);

    public static <T> Event<T> createArrayBacked(Function<List<T>, T> invokerFactory) {
        return new ArrayBackedEvent<>(invokerFactory);
    }

    static class ArrayBackedEvent<T> implements Event<T> {

        private final Function<List<T>, T> invokerFactory;

        private List<T> listeners = new ArrayList<>();
        private T invoker;

        public ArrayBackedEvent(Function<List<T>, T> invokerFactory) {
            this.invokerFactory = invokerFactory;
            this.invoker = invokerFactory.apply(Collections.emptyList());
        }

        @Override
        public T invoker() {
            return invoker;
        }

        @Override
        public void addListener(T listener) {
            listeners.add(listener);
            invoker = invokerFactory.apply(listeners);
        }
        
    }
}
