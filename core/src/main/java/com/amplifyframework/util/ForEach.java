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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class that reduces boilerplate code in tests
 * where an oft-used pattern of mapping a list of values to another by
 * applying a function.
 */
public final class ForEach {
    private ForEach() {}

    /**
     * Creates a list of values by applying a given function to the
     * values provided in an input collection.
     * @param inputCollection The collection with the input values.
     * @param mapping The mapping function.
     * @param <I> The input type.
     * @param <O> The output type.
     * @return The list containing the results of the mapping operation for each input value.
     */
    public static <I, O> List<O> inCollection(Collection<I> inputCollection, Mapping<I, O> mapping) {
        final List<O> out = new ArrayList<>();
        for (I item : inputCollection) {
            out.add(mapping.apply(item));
        }
        return Immutable.of(out);
    }

    /**
     * Creates a list of values by applying a given function to the
     * values provided in an input array.
     * @param inputCollection The array with the input values.
     * @param mapping The mapping function.
     * @param <I> The input type.
     * @param <O> The output type.
     * @return The list containing the results of the mapping operation for each input value.
     */
    public static <I, O> List<O> inArray(I[] inputCollection, Mapping<I, O> mapping) {
        final List<O> out = new ArrayList<>();
        for (I item : inputCollection) {
            out.add(mapping.apply(item));
        }
        return Immutable.of(out);
    }

    /**
     * Interface created to allow callers to use functional-style parameters.
     * @param <I> The input type.
     * @param <O> The output type.
     */
    @FunctionalInterface
    public interface Mapping<I, O> {
        /**
         * Function that maps a given input to an output value.
         * @param inputValue The input value.
         * @return The result of the function.
         */
        O apply(I inputValue);
    }
}
