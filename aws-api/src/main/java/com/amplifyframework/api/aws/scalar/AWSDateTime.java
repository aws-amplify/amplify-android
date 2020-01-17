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

package com.amplifyframework.api.aws.scalar;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.TimeZone;

/**
 * DateTime instance to accommodate the specifications of
 * AppSync Scalar AWSDateTime data type.
 *
 * Format: YYYY-MM-DDThh:mm:ss.sss[Z|±hh:mm:ss]
 *
 * Note: Timezone offset is compulsory.
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync Defined Scalars</a>
 */
public final class AWSDateTime extends AWSDate {
    private final AWSTime time;

    /**
     * Constructs an instance of AWSDateTime.
     * @param timezone Timezone to associate with this datetime
     * @param time Milliseconds past since UNIX Epoch
     */
    public AWSDateTime(@NonNull TimeZone timezone, long time) {
        super(timezone, time);
        this.time = new AWSTime(timezone, time);
    }

    /**
     * Returns the hour value stored in this time.
     * @return the hour value
     */
    public int getHour() {
        return time.getHour();
    }

    /**
     * Returns the minute value stored in this time.
     * @return the minute value
     */
    public int getMinute() {
        return time.getMinute();
    }

    /**
     * Returns the second value stored in this time.
     * @return the second value
     */
    public int getSecond() {
        return time.getSecond();
    }

    /**
     * Returns the millisecond value stored in this time.
     * @return the millisecond value
     */
    public int getMillisecond() {
        return time.getMillisecond();
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.US, "%04d-%02d-%02dT%s",
                getYear(),
                getMonth() + 1,
                getDayOfMonth(),
                time.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AWSDateTime)) {
            return false;
        }

        AWSDateTime that = (AWSDateTime) obj;
        return this.getTime() == that.getTime();
    }
}
