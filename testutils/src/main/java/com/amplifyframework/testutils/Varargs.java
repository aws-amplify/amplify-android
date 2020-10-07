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

package com.amplifyframework.testutils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility to convert an (possibly null) array into
 * an immutable list.
 */
public final class Varargs {
    private Varargs() {}

    /**
     * Converts a (possibly null) array into a non-null (possibly empty) list.
     * @param items A possibly null array of items
     * @param <T> Type of items in the array
     * @return A non-null, possibly empty collection
     */
    @NonNull
    public static <T> List<T> toList(@Nullable T[] items) {
        List<T> safeItems = new ArrayList<>();
        if (items != null) {
            safeItems.addAll(Arrays.asList(items));
        }
        return Immutable.of(safeItems);
    }
}
