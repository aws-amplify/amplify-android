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

import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.testutils.Sleep;
import com.amplifyframework.util.Time;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test that asset loader properly blocks operations
 * until load completion.
 */
public final class LatchedLoaderTest {
    private static final int TIMEOUT_MS = 1000;
    private static final long UNIT_TIME_MS = 100;

    /**
     * Test that latched load will only be completed after
     * all of its component tasks finish loading.
     * @throws Exception if latched load fails or times out
     */
    @Test(timeout = TIMEOUT_MS)
    public void testLatchedCompoundLoaderWaitsForCompletion() throws Exception {
        // Make 5 mock loadable instances, each with load duration of
        // 100ms, 200ms, 300ms, 400ms, and 500ms, respectively.
        final int count = 5;
        List<Loadable<?, PredictionsException>> assets = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            final long loadDuration = i * UNIT_TIME_MS;
            assets.add(new FakeLoadable(loadDuration));
        }

        // Create latched compound loader with mock loadable instances
        LatchedCompoundLoader loader = LatchedCompoundLoader.with(assets);
        long startTime = Time.now();
        loader.start();
        loader.await();
        long endTime = Time.now();

        // Assert every asset is loaded
        for (Loadable<?, PredictionsException> asset : assets) {
            assertTrue(asset.isLoaded());
        }

        // Assert the entire load took at least 500ms
        assertTrue("Load completed earlier than its tasks.",
                endTime - startTime > count * UNIT_TIME_MS);
    }

    /**
     * Test that latched load will await load completion even if
     * the load task was not started yet.
     * @throws Exception if latched load fails or times out
     */
    @Test(timeout = TIMEOUT_MS)
    public void testStartCanBeCalledAfterAwait() throws Exception {
        // Create a loadable that takes 400ms to load
        final int loadDuration = 400;
        final int waitDuration = 100;
        Loadable<String, PredictionsException> loadable = new FakeLoadable(loadDuration);
        LatchedCompoundLoader loader = LatchedCompoundLoader.with(loadable);

        // Start a thread that sleeps for 100ms
        // before starting loader.
        long startTime = Time.now();
        new Thread(() -> {
            Sleep.milliseconds(waitDuration);
            loader.start();
        }).start();

        // Await immediately after starting the thread.
        // Loader will start 100ms from now.
        loader.await();
        long endTime = Time.now();

        // Assert that the loadable is loaded
        assertTrue(loadable.isLoaded());

        // Assert the entire load took at least 400ms + 100ms
        assertTrue("Load completed earlier than its tasks.",
                endTime - startTime >= waitDuration + loadDuration);
    }
}
