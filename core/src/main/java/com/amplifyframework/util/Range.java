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

/**
 * Range of values.
 * @param <T> Generics type of the Range, which should conform to Comparable.
 */
public class Range<T extends Comparable<T>> {

    private T low;
    private T high;

    /**
     * Constructs the Range object.
     * @param low Low inclusive value of the range
     * @param high High inclusive value of the range
     * @throws IllegalArgumentException Exception thrown if the arguments are illegal.
     */
    public Range(T low, T high) throws IllegalArgumentException {
        if (low.compareTo(high) > 0) {
            throw new IllegalArgumentException("Low value should be lower than high");
        }
        this.low = low;
        this.high = high;
    }

    /**
     * Checks if the given value is inside the range.
     * @param value Value to check
     * @return True if the value is inside the range.
     */
    public boolean contains(T value) {
        return value.compareTo(low) >= 0 && value.compareTo(high) <= 0;
    }
}
