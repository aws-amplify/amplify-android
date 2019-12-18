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

package com.amplifyframework.util;

import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Utility for common Array operations.
 */
public final class ArrayUtils {

    /**
     * Dis-allows instantiation of this class.
     */
    private ArrayUtils() { }

    /**
     * Creates a copy of the provided source array.
     * @param sourceArray A possibly null, possibly empty array
     * @param <T> The type of items in the array
     * @return A copy of the provided array
     */
    public static <T> T[] copyOf(@Nullable final T[] sourceArray) {
        if (sourceArray == null) {
            return null;
        }

        return Arrays.copyOf(sourceArray, sourceArray.length);
    }
}
