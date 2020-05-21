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

import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A utility to create scattered copies of ordered collections.
 */
public final class Scattered {
    private Scattered() {}

    /**
     * Create a copy of the provided list, but scatter its contents into a random ordering.
     * This has a few nuances:
     *
     *   1. A null, empty, or singleton list cannot be scattered; just return an immutable
     *      copy, immediately.
     *
     *   2. For lists that have more than 1 element, we can sort the list using a comparator
     *      that decides at random whether one element is larger than another. A SecureRandom
     *      is used for this, to reduce the likelihood that the same ordering will be generated
     *      across multiple runs.
     *
     *   3. For lists with small amounts of elements, the likelihood of attaining a unique
     *      list from a single, random sorting, is quite low. For a list of two, its 50%.
     *      To guarantee that the output is also _unique_, the sort operation is performed infinitely,
     *      until the output is guaranteed to be different than the input.
     * @param original A possibly-null, possibly-empty, list of items, in no particular order
     * @param <T> Type of item in list
     * @return A scattered copy of the list of the provided items, abiding the rules mentioned above
     */
    @Nullable
    public static <T> List<T> list(@Nullable List<T> original) {
        if (original == null || original.size() <= 1) {
            return Immutable.of(original);
        }
        SecureRandom random = new SecureRandom();
        List<T> scattered = new ArrayList<>(original);
        while (scattered.equals(original)) {
            //noinspection ComparatorMethodParameterNotUsed Intentional; result is random
            Collections.sort(scattered, (one, two) -> random.nextInt());
        }
        return Immutable.of(scattered);
    }
}
