/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains methods for immutability.
 */
public final class Immutable {
    private Immutable() {
    }

    /**
     * Create an immutable copy of the map passed in.
     *
     * @param mutableMap the input map for which an immutable map
     *                   is created and returned.
     * @param <K> the key type of the mutableMap.
     * @param <V> the value type of the mutableMap.
     * @return the immutable copy of the mutableMap.
     */
    public static <K, V> Map<K, V> of(@Nullable final Map<K, V> mutableMap) {
        if (mutableMap == null) {
            return null;
        }

        final Map<K, V> copy = new HashMap<>(mutableMap);
        return Collections.unmodifiableMap(copy);
    }
}
