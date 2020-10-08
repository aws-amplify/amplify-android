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

package com.amplifyframework.datastore.syncengine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link TimeBasedUuidTypeAdapter}.
 */
public final class TimeBasedUuidTypeAdapterTest {
    /**
     * Tests the functionality of just the {@link TimeBasedUuidTypeAdapter}
     * in isolation to other parts of the {@link GsonPendingMutationConverter}.
     */
    @Test
    public void canConvertTimeBasedUuid() {
        TimeBasedUuid original = TimeBasedUuid.create();

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(TimeBasedUuid.class, new TimeBasedUuidTypeAdapter())
            .create();

        String json = gson.toJson(original);
        TimeBasedUuid reconstructed = gson.fromJson(json, TimeBasedUuid.class);

        assertEquals(original, reconstructed);
    }
}
