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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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
public final class AWSDateTime extends AWSTemporal {

    private static final Set<Integer> COMPONENTS =
        new HashSet<>(Arrays.asList(
            Calendar.YEAR,
            Calendar.MONTH,
            Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY,
            Calendar.MINUTE,
            Calendar.SECOND,
            Calendar.MILLISECOND,
            Calendar.ZONE_OFFSET
        ));

    /**
     * Instantiate an AWSDateTime from another AWSTemporal instance.
     * @param scalar An instance of AWSTemporal class
     */
    public AWSDateTime(@NonNull AWSTemporal scalar) {
        super(scalar);
    }

    /**
     * Instantiate an AWSDateTime from {@link Date} with UTC timezone.
     * @param date Java 7 Date instance
     */
    public AWSDateTime(@NonNull Date date) {
        super(date);
    }

    /**
     * Instantiate an AWSDateTime from {@link Date} with specific
     * timezone to associate.
     * @param date Java 7 Date instance
     * @param timezone Timezone to associate with this date
     */
    public AWSDateTime(@NonNull Date date,
                       @NonNull TimeZone timezone) {
        super(date, timezone);
    }

    /**
     * Returns the year value stored in this datetime.
     * @return the year value
     */
    public int getYear() {
        return get(Calendar.YEAR);
    }

    /**
     * Returns the month value stored in this datetime.
     * @return the month value
     */
    public int getMonth() {
        return get(Calendar.MONTH);
    }

    /**
     * Returns the day of month value stored in this datetime.
     * @return the day of month value
     */
    public int getDayOfMonth() {
        return get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Returns the hour value stored in this datetime.
     * @return the hour value
     */
    public int getHour() {
        return get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Returns the minute value stored in this datetime.
     * @return the minute value
     */
    public int getMinute() {
        return get(Calendar.MINUTE);
    }

    /**
     * Returns the second value stored in this datetime.
     * @return the second value
     */
    public int getSecond() {
        return get(Calendar.SECOND);
    }

    /**
     * Returns the millisecond value stored in this datetime.
     * @return the millisecond value
     */
    public int getMillisecond() {
        return get(Calendar.MILLISECOND);
    }

    /**
     * Creates an instance of AWSDateTime representing current time.
     * @return an instance of AWSDateTime representing current time
     */
    @NonNull
    public static AWSDateTime now() {
        return new AWSDateTime(
                new Date(System.currentTimeMillis()),
                TimeZone.getDefault()
        );
    }

    /**
     * Parses a string following "YYYY-MM-DDThh:mm:ss:SSSZ" format into an
     * instance of {@link AWSDateTime} with equivalent UNIX epoch time.
     * @param date String to be parsed
     * @return Instance of AWSDateTime
     * @throws ParseException if an error is encountered while parsing
     */
    @NonNull
    @SuppressWarnings("MagicNumber")
    public static AWSDateTime parse(@NonNull String date) throws ParseException {
        ParsePosition pos = new ParsePosition(0);

        int second = 0;
        int millisecond = 0;

        int year = parseInt(date, pos, 4);
        if (!verifyCharAt(date, pos, '-')) {
            throw new ParseException("Expected `-`.", pos.getIndex());
        }

        int month = parseInt(date, pos, 2);
        if (!verifyCharAt(date, pos, '-')) {
            throw new ParseException("Expected `-`.", pos.getIndex());
        }

        int day = parseInt(date, pos, 2);
        if (!verifyCharAt(date, pos, 'T')) {
            throw new ParseException("Expected `T`.", pos.getIndex());
        }

        int hour = parseInt(date, pos, 2);
        if (!verifyCharAt(date, pos, ':')) {
            throw new ParseException("Expected `:`.", pos.getIndex());
        }

        int minute = parseInt(date, pos, 2);

        // seconds and milliseconds fields are optional
        if (pos.getIndex() < date.length() && verifyCharAt(date, pos, ':')) {
            second = parseInt(date, pos, 2);
            if (pos.getIndex() < date.length() && verifyCharAt(date, pos, '.')) {
                millisecond = parseInt(date, pos, 3);
            }
        }

        TimeZone timezone = parseTimeZone(date, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setLenient(false);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);

        return new AWSDateTime(calendar.getTime());
    }

    @NonNull
    @Override
    Set<Integer> getCalendarComponentFields() {
        return COMPONENTS;
    }

    @Override
    @NonNull
    public String toString() {
        int second = getSecond();
        int millis = getMillisecond();
        String timezone = formatTimeZone(getTimeZone());

        String optional;
        if (millis > 0) {
            optional = String.format(Locale.US, ":%02d.%03d", second, millis);
        } else if (second > 0) {
            optional = String.format(Locale.US, ":%02d", second);
        } else {
            optional = "";
        }

        return String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d%s%s",
                getYear(),
                getMonth() + 1,
                getDayOfMonth(),
                getHour(),
                getMinute(),
                optional,
                timezone);
    }
}
