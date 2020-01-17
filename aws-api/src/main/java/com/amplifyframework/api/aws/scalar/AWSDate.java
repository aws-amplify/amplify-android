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

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date instance to accommodate the specifications of
 * AppSync Scalar AWSDate data type.
 *
 * Format: YYYY-MM-DD[Z|±hh:mm:ss]
 *
 * Note: Timezone offset is optional. Local timezone
 * will be appended and stored if none is provided.
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync Defined Scalars</a>
 */
public class AWSDate extends AWSTemporal {
    /**
     * Constructs an instance of AWSDate.
     * @param timezone Timezone to associate with this date
     * @param time Milliseconds past since UNIX Epoch
     */
    public AWSDate(@NonNull TimeZone timezone, long time) {
        super(timezone, time);
    }

    /**
     * Returns the year value stored in this date.
     * @return the year value
     */
    public int getYear() {
        return get(Calendar.YEAR);
    }

    /**
     * Returns the month value stored in this date.
     * @return the month value
     */
    public int getMonth() {
        return get(Calendar.MONTH);
    }

    /**
     * Returns the day of month value stored in this date.
     * @return the day of month value
     */
    public int getDayOfMonth() {
        return get(Calendar.DAY_OF_MONTH);
    }

    @Override
    @NonNull
    public String toString() {
        String timezone = AWSDateTimeUtils.format(getTimeZone());
        return String.format(Locale.US, "%04d-%02d-%02d%s",
                getYear(),
                getMonth() + 1,
                getDayOfMonth(),
                timezone);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AWSDate)) {
            return false;
        }

        AWSDate that = (AWSDate) obj;
        if (this.getTimeZone().getRawOffset() != that.getTimeZone().getRawOffset()) {
            return false;
        }
        if (this.getYear() != that.getYear()) {
            return false;
        }
        if (this.getMonth() != that.getMonth()) {
            return false;
        }
        return this.getDayOfMonth() == that.getDayOfMonth();
    }
}
