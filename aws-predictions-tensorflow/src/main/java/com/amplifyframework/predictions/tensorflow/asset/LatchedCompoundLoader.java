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
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Latch;
import com.amplifyframework.predictions.PredictionsException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads multiple {@link Loadable} assets and waits for its completion.
 */
public final class LatchedCompoundLoader implements Latch<PredictionsException> {
    private final List<Loadable<?, PredictionsException>> assets;
    private final CountDownLatch loaded;
    private final AtomicReference<PredictionsException> failed;

    private LatchedCompoundLoader(List<Loadable<?, PredictionsException>> assets) {
        this.assets = assets;
        this.loaded = new CountDownLatch(assets.size());
        this.failed = new AtomicReference<>();

        // Attach latch countdown
        for (Loadable<?, PredictionsException> asset : this.assets) {
            asset.onLoaded(onLoad -> this.loaded.countDown(), this.failed::set);
        }
    }

    /**
     * Constructs an instance of latched loader containing multiple loadable
     * assets. The process will be blocked until load is completed.
     * @param assets the list of assets to do a blocking load on
     * @return the latched loader to load every specified asset
     * @throws IllegalArgumentException if assets list is empty
     */
    public static LatchedCompoundLoader with(@NonNull List<Loadable<?, PredictionsException>> assets) {
        if (Objects.requireNonNull(assets).isEmpty()) {
            throw new IllegalArgumentException("Requires at least one loadable asset.");
        }

        // Check that they are all non-null
        for (Loadable<?, PredictionsException> asset : assets) {
            Objects.requireNonNull(asset);
        }

        return new LatchedCompoundLoader(assets);
    }

    /**
     * Constructs an instance of latched loader containing multiple loadable
     * assets. The process will be blocked until load is completed.
     * @param assets the list of assets to do a blocking load on
     * @return the latched loader to load every specified asset
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static LatchedCompoundLoader with(@NonNull Loadable... assets) {
        return with(Arrays.asList(assets));
    }

    /**
     * Start loading all of the assets.
     */
    @WorkerThread
    public synchronized void start() {
        for (Loadable<?, PredictionsException> asset : this.assets) {
            asset.load();
        }
    }

    /**
     * Latch onto load task and wait for its completion.
     * Escapes early if an exception was already emitted by the task.
     * @throws PredictionsException if load failed or was interrupted
     */
    public void await() throws PredictionsException {
        if (failed.get() != null) {
            throw failed.get();
        }

        try {
            loaded.await();
        } catch (InterruptedException exception) {
            throw new PredictionsException(
                    "Service initialization was interrupted.",
                    "Please wait for the required assets to be fully loaded."
            );
        }
    }
}
