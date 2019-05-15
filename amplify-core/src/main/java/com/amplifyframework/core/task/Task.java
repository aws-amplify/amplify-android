package com.amplifyframework.core.task;

import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.async.EventCallback;

public interface Task<R> {
    void registerCallback(final Callback<R> callback);

    void registerEventCallback(final EventCallback eventCallback);
}
