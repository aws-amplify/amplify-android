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

package com.amplifyframework.core.types.scalar;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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
public final class AWSTime extends AWSTemporal {

    private static final Set<Integer> COMPONENTS = new HashSet<>();

    static {
        COMPONENTS.add(Calendar.YEAR);
        COMPONENTS.add(Calendar.MONTH);
        COMPONENTS.add(Calendar.DAY_OF_MONTH);
        COMPONENTS.add(Calendar.HOUR_OF_DAY);
        COMPONENTS.add(Calendar.MINUTE);
        COMPONENTS.add(Calendar.SECOND);
        COMPONENTS.add(Calendar.MILLISECOND);
        COMPONENTS.add(Calendar.ZONE_OFFSET);
    }

    /**
     * Instantiate an AWSTime from another AWSTemporal instance.
     * @param scalar An instance of AWSTemporal class
     */
    public AWSTime(@NonNull AWSTemporal scalar) {
        super(scalar);
    }

    /**
     * Instantiate an AWSTime from {@link Date} with UTC timezone.
     * @param date Java 7 Date instance
     */
    public AWSTime(@NonNull Date date) {
        super(date);
    }

    /**
     * Instantiate an AWSTime from {@link Date} with specific
     * timezone to associate.
     * @param date Java 7 Date instance
     * @param timezone Timezone to associate with this date
     */
    public AWSTime(@NonNull Date date,
                   @NonNull TimeZone timezone) {
        super(date, timezone);
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

    /**
     * Parses a string following "hh:mm:ss.SSSZ" format into an
     * instance of {@link AWSTime} with equivalent UNIX epoch time.
     * @param time String to be parsed
     * @return Instance of AWSTime
     * @throws ParseException if an error is encountered while parsing
     */
    @NonNull
    @SuppressWarnings("MagicNumber")
    public static AWSTime parse(@NonNull String time) throws ParseException {
        ParsePosition pos = new ParsePosition(0);

        int second = 0;
        int millisecond = 0;

        int hour = parseInt(time, pos, 2);
        if (!verifyCharAt(time, pos, ':')) {
            throw new ParseException("Expected `:`.", pos.getIndex());
        }

        int minute = parseInt(time, pos, 2);

        // seconds and milliseconds fields are optional
        if (pos.getIndex() < time.length() && verifyCharAt(time, pos, ':')) {
            second = parseInt(time, pos, 2);
            if (pos.getIndex() < time.length() && verifyCharAt(time, pos, '.')) {
                millisecond = parseInt(time, pos, 3);
            }
        }

        TimeZone timezone = parseTimeZone(time, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.clear();
        calendar.setLenient(false);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);

        return new AWSTime(calendar.getTime(), calendar.getTimeZone());
    }

    @Override
    @NonNull
    Set<Integer> getCalendarComponentFields() {
        return COMPONENTS;
    }

    @Override
    @NonNull
    public String toString() {
        int hour = getHour();
        int minute = getMinute();
        int second = getSecond();
        int millis = getMillisecond();
        String timezone = formatTimeZone(getTimeZone());

        if (millis > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d.%03d%s", hour, minute, second, millis, timezone);
        }
        if (second > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d%s", hour, minute, second, timezone);
        }
        return String.format(Locale.US, "%02d:%02d%s", hour, minute, timezone);
    }
}
