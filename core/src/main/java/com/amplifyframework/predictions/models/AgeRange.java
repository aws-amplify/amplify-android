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

package com.amplifyframework.predictions.models;

/**
 * Class to represent a range of age.
 */
public final class AgeRange {
    private final int low;
    private final int high;

    /**
     * Constructs a new instance of AgeRange.
     * @param low minimum value of the range
     * @param high maximum value of the range
     * @throws IllegalArgumentException if low > high
     */
    public AgeRange(int low, int high) {
        if (high < low) {
            throw new IllegalArgumentException("Low cannot be " +
                    "higher than High.");
        }
        this.low = low;
        this.high = high;
    }

    /**
     * Gets the minimum value.
     * @return the minimum value
     */
    public int getLow() {
        return low;
    }

    /**
     * Gets the maximum value.
     * @return the maximum value
     */
    public int getHigh() {
        return high;
    }

    /**
     * Return true if age is between this range, inclusive.
     * @param age age to check
     * @return true if age is between the range, inclusive
     */
    public boolean contains(int age) {
        return age >= low && age <= high;
    }
}
