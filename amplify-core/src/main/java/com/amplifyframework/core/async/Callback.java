package com.amplifyframework.core.async;

/**
 * Callback async operations.
 * @param <Result>
 */
public interface Callback<Result> {

    void onResult(Result result);

    void onError(Exception e);
}