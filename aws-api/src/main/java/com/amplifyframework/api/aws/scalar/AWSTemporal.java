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
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Abstract class to remember both the time and associated timezone.
 * This class is essentially a wrapper around {@link GregorianCalendar}
 * with easy-to-access methods for relevant fields.
 *
 * This class was created to avoid the usage of a third-party library
 * or Java 8 features to represent date/time data. Java.util.Date
 * class will lose the information regarding timezone, which made it
 * unsuitable for representing an AppSync scalar.
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync Defined Scalars</a>
 */
public abstract class AWSTemporal implements Comparable<AWSTemporal> {
    private final Calendar calendar;

    AWSTemporal(long time) {
        this(TimeZone.getDefault(), time);
    }

    AWSTemporal(@NonNull final TimeZone timezone, long time) {
        Objects.requireNonNull(timezone);
        this.calendar = new GregorianCalendar(timezone);
        this.calendar.setLenient(false);
        this.calendar.setTimeInMillis(time);
    }

    /**
     * Gets the {@link Calendar} field value.
     * @param field Field to evaluate
     * @return Corresponding value inside calendar
     */
    protected int get(int field) {
        return calendar.get(field);
    }

    /**
     * Returns the UNIX Epoch time in milliseconds.
     * @return the UNIX Epoch time
     */
    @NonNull
    public long getTime() {
        return calendar.getTimeInMillis();
    }

    /**
     * Returns the associated instance of timezone.
     * This timezone could potentially be a custom extended
     * timezone that allows offset with seconds precision.
     * Such timezone will have the ID of
     * {@link AWSDateTimeUtils#EXTENDED_TIMEZONE_ID}.
     * @return the associated instance of timezone
     */
    @NonNull
    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    /**
     * Returns the formatted string representing the
     * temporal instance. The format will be compliant with
     * the specifications of AWS AppSync Scalar types.
     * @return the formatted string
     */
    @Override
    @NonNull
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public final int compareTo(AWSTemporal obj) {
        return (int) (this.getTime() - obj.getTime());
    }

    @Override
    public final int hashCode() {
        return (int) getTime();
    }
}
