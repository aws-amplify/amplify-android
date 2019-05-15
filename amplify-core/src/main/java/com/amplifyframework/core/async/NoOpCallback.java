package com.amplifyframework.core.async;

public class NoOpCallback<R> implements Callback<R>{
    @Override
    public void onResult(R result) {

    }

    @Override
    public void onError(Exception e) {

    }
}
