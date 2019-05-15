package com.amplifyframework.storage;

import android.support.annotation.NonNull;

import com.amplifyframework.core.async.AsyncOperation;
import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.provider.Provider;
import com.amplifyframework.core.task.Options;
import com.amplifyframework.core.task.Result;

public class StorageOperation implements AsyncOperation {
    @Override
    public StorageOperation callback(@NonNull Callback<? extends Result> callback) {
        return this;
    }

    @Override
    public StorageOperation options(@NonNull Options options) {
        return this;
    }

    @Override
    public StorageOperation provider(Class<? extends Provider> providerClass) {
        return null;
    }

    @Override
    public StorageOperation start() {
        return this;
    }

    @Override
    public StorageOperation pause() {
        return this;
    }

    @Override
    public StorageOperation resume() {
        return this;
    }

    @Override
    public StorageOperation cancel() {
        return this;
    }
}
