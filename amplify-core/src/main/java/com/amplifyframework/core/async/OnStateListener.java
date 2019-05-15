package com.amplifyframework.core.async;

import com.amplifyframework.core.task.State;

public interface OnStateListener {
    void onStateChanged(State state);
}
