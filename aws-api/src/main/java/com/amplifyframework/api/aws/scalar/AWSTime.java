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
 * Time instance to accommodate the specifications of
 * AppSync Scalar AWSTime data type.
 *
 * Format: hh:mm:ss.sss[Z|±hh:mm:ss]
 *
 * Note: Timezone offset is optional. Local timezone
 * will be appended and stored if none is provided.
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync Defined Scalars</a>
 */
public class AWSTime extends AWSTemporal {
    /**
     * Constructs an instance of AWSTime.
     * @param timezone Timezone to associate with this date
     * @param time Milliseconds past since UNIX Epoch
     */
    public AWSTime(final TimeZone timezone, long time) {
        super(timezone, time);
    }

    /**
     * Returns the hour value stored in this time.
     * @return the hour value
     */
    public int getHour() {
        return get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Returns the minute value stored in this time.
     * @return the minute value
     */
    public int getMinute() {
        return get(Calendar.MINUTE);
    }

    /**
     * Returns the second value stored in this time.
     * @return the second value
     */
    public int getSecond() {
        return get(Calendar.SECOND);
    }

    /**
     * Returns the millisecond value stored in this time.
     * @return the millisecond value
     */
    public int getMillisecond() {
        return get(Calendar.MILLISECOND);
    }

    @Override
    @NonNull
    public String toString() {
        int hour = getHour();
        int minute = getMinute();
        int second = getSecond();
        int millis = getMillisecond();
        String timezone = AWSDateTimeUtils.format(getTimeZone());

        if (millis > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d.%03d%s", hour, minute, second, millis, timezone);
        }
        if (second > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d%s", hour, minute, second, timezone);
        }
        return String.format(Locale.US, "%02d:%02d%s", hour, minute, timezone);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AWSTime)) {
            return false;
        }

        AWSTime that = (AWSTime) obj;
        if (this.getTimeZone().getRawOffset() != that.getTimeZone().getRawOffset()) {
            return false;
        }
        if (this.getHour() != that.getHour()) {
            return false;
        }
        if (this.getMinute() != that.getMinute()) {
            return false;
        }
        if (this.getSecond() != that.getSecond()) {
            return false;
        }
        return this.getMillisecond() == that.getMillisecond();
    }
}
