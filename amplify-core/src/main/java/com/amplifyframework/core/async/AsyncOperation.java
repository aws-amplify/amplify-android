package com.amplifyframework.core.async;

import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.task.Options;
import com.amplifyframework.core.task.Result;

public interface AsyncOperation {
    AsyncOperation options(Options options);

    AsyncOperation plugin(Class<? extends Plugin> pluginClass);

    AsyncOperation start();

    AsyncOperation pause();

    AsyncOperation resume();

    AsyncOperation cancel();
}
