package com.amplifyframework.core.async;

import com.amplifyframework.core.task.AmplifyEvent;

public interface EventCallback extends Callback {
    void onEvent(AmplifyEvent event);
}
