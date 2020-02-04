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
public final class AWSDate extends AWSTemporal {

    private static final Set<Integer> COMPONENTS = new HashSet<>();

    static {
        COMPONENTS.add(Calendar.YEAR);
        COMPONENTS.add(Calendar.MONTH);
        COMPONENTS.add(Calendar.DAY_OF_MONTH);
        COMPONENTS.add(Calendar.ZONE_OFFSET);
    }

    /**
     * Instantiate an AWSDate from another AWSTemporal instance.
     * @param scalar An instance of AWSTemporal class
     */
    public AWSDate(@NonNull AWSTemporal scalar) {
        super(scalar);
    }

    /**
     * Instantiate an AWSDate from {@link Date} with UTC timezone.
     * @param date Java 7 Date instance
     */
    public AWSDate(@NonNull Date date) {
        super(date);
    }

    /**
     * Instantiate an AWSDate from {@link Date} with specific
     * timezone to associate.
     * @param date Java 7 Date instance
     * @param timezone Timezone to associate with this date
     */
    public AWSDate(@NonNull Date date,
                   @NonNull TimeZone timezone) {
        super(date, timezone);
    }

    @Override
    @NonNull
    Set<Integer> getCalendarComponentFields() {
        return COMPONENTS;
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

    /**
     * Parses a string following "YYYY-MM-DDZ" format into an
     * instance of {@link AWSDate} with equivalent UNIX epoch time.
     * @param date String to be parsed
     * @return Instance of AWSDate
     * @throws ParseException if an error is encountered while parsing
     */
    @NonNull
    @SuppressWarnings("MagicNumber")
    public static AWSDate parse(@NonNull String date) throws ParseException {
        ParsePosition pos = new ParsePosition(0);

        int year = parseInt(date, pos, 4);
        if (!verifyCharAt(date, pos, '-')) {
            throw new ParseException("Expected `-`.", pos.getIndex());
        }

        int month = parseInt(date, pos, 2);
        if (!verifyCharAt(date, pos, '-')) {
            throw new ParseException("Expected `-`.", pos.getIndex());
        }

        int day = parseInt(date, pos, 2);

        TimeZone timezone = parseTimeZone(date, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setLenient(false);
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return new AWSDate(calendar.getTime(), calendar.getTimeZone());
    }

    @Override
    @NonNull
    public String toString() {
        String timezone = formatTimeZone(getTimeZone());
        return String.format(Locale.US, "%04d-%02d-%02d%s",
                getYear(),
                getMonth() + 1,
                getDayOfMonth(),
                timezone);
    }
}
