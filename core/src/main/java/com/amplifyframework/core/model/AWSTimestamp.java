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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * The AWSTimestamp scalar type represents the number of seconds that have elapsed
 * since 1970-01-01T00:00Z. Timestamps are serialized and deserialized as numbers.
 * Negative values are also accepted and these represent the number of seconds
 * til 1970-01-01T00:00Z.
 */
public final class AWSTimestamp {
    private final long secondsSinceEpoch;

    /**
     * Constructs a new AWSTimestamp that represents the current system time.
     */
    public AWSTimestamp() {
        this(new Date());
    }

    /**
     * Constructs a new AWSTimestamp, as an amount of time since the UNIX epoch.
     * @param timeSinceEpoch An amount of time that has elapsed since the UNIX epoch,
     *                       for example: 1_588_703_119L seconds. The unit for this value
     *                       must be passed in the second argument.
     * @param timeUnit The unit in which the first argument is expressed. For example,
     *                 if the first argument is 1_588_703_119L, that would represent the
     *                 number of seconds between the UNIX epoch and
     *                 Tuesday, May 5, 2020 6:25:19 PM in GMT.
     */
    public AWSTimestamp(long timeSinceEpoch, TimeUnit timeUnit) {
        this.secondsSinceEpoch = timeUnit.toSeconds(timeSinceEpoch);
    }

    /**
     * Constructs an AWSTimestamp from a Date.
     * @param date A date, that will be interrogated for the current UNIX time;
     *             any sub-second precision contained in the Date will be discarded
     */
    public AWSTimestamp(@NonNull Date date) {
        this(date.getTime(), TimeUnit.MILLISECONDS);
    }

    public long getSecondsSinceEpoch() {
        return this.secondsSinceEpoch;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AWSTimestamp that = (AWSTimestamp) thatObject;

        return secondsSinceEpoch == that.secondsSinceEpoch;
    }

    @Override
    public int hashCode() {
        return (int) (secondsSinceEpoch ^ (secondsSinceEpoch >>> 32));
    }

    @Override
    public String toString() {
        return "AWSTimestamp{" +
            "timestamp=" + secondsSinceEpoch +
            '}';
    }
}
