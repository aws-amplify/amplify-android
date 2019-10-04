package com.amplifyframework.core.async;

public interface EventListener<T> {
    void onEvent(T event);
}
