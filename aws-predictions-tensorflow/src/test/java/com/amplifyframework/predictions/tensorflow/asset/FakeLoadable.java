/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.predictions.tensorflow.asset;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.testutils.Sleep;
import com.amplifyframework.testutils.random.RandomString;

/**
 * Simple fake loadable class that completes load after
 * a pre-determined amount of time.
 */
final class FakeLoadable implements Loadable<String, PredictionsException> {
    private final long loadDuration;

    private Consumer<String> onLoaded;
    private boolean loaded;

    /**
     * Constructs a new instance of {@link FakeLoadable}
     * with the given load duration.
     * @param millis the amount of time that passes before
     *               load completes after being called
     */
    FakeLoadable(long millis) {
        this.loadDuration = millis;
    }

    @Override
    public void load() {
        new Thread(() -> {
            Sleep.milliseconds(loadDuration);
            onLoaded.accept(RandomString.string());
            loaded = true;
        }).start();
    }

    @Override
    public void unload() {
        // Do nothing.
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public Loadable<String, PredictionsException> onLoaded(
            Consumer<String> onLoaded,
            Consumer<PredictionsException> onLoadError
    ) {
        this.onLoaded = onLoaded;
        return this;
    }

    @Override
    public Loadable<String, PredictionsException> onUnloaded(
            Action onUnloaded,
            Consumer<PredictionsException> onUnloadError
    ) {
        return this;
    }

    @NonNull
    @Override
    public String getValue() {
        return RandomString.string();
    }
}
