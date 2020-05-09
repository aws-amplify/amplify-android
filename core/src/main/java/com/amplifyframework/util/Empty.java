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

package com.amplifyframework.util;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Utility for common Collection operations which are not otherwise available.
 */
public final class Empty {
    private Empty() {}

    /**
     * Returns true if collection is null, or if it is instantiated but not populated.
     * @param collection instance of collection to check
     * @return true if collection is null, or if it is instantiated but not populated
     */
    public static boolean check(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns true if map is null, or if it is instantiated but not populated.
     * @param map instance of map to check
     * @return true if map is null, or if it is instantiated but not populated
     */
    public static boolean check(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
