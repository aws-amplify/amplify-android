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

package com.amplifyframework.api.aws.internal;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class containing the logic to parse and serialize {@link Date}
 * such that it is compatible with the format of AWSDate and AWSDateTime.
 */
public final class AWSDateTimeUtils {
    private static final String UTC_ID = "UTC";
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(UTC_ID);

    private static final int MINUTES_IN_ONE_HOUR = 60;
    private static final int SECONDS_IN_ONE_HOUR = 60;
    private static final int MILLIS_IN_ONE_SECOND = 1000;

    private static final String DATE_REGEX_EXPRESSION =
            "(?:([0-9]{4})-(1[0-2]|[0][1-9])-(3[01]|[12][0-9]|0[1-9]))";
    private static final String TIME_REGEX_EXPRESSION =
            "(?:T(2[0-3]|[01][0-9]):([0-5][0-9])(?::([0-5][0-9])(?:.([0-9]{3}))?)?)";
    private static final String ZONE_REGEX_EXPRESSION =
            "(?:Z|([+-])(2[0-3]|[01][0-9])(?::([0-5][0-9])(?::([0-5][0-9]))?)?)";

    private static final Pattern DATE_PATTERN = Pattern.compile(DATE_REGEX_EXPRESSION);
    private static final Pattern TIME_PATTERN = Pattern.compile(TIME_REGEX_EXPRESSION);
    private static final Pattern ZONE_PATTERN = Pattern.compile(ZONE_REGEX_EXPRESSION);

    /**
     * Dis-allows instantiation of this class.
     */
    private AWSDateTimeUtils() { }

    /**
     * Format date to be compliant with AWS AppSync Scalar types.
     * The system default time zone will be applied.
     *
     * @param date the date to format
     * @return the date formatted as to be AppSync Scalar type compliant
     */
    public static String format(Date date) {
        return format(date, TimeZone.getDefault());
    }

    /**
     * Format date to be compliant with AWS AppSync Scalar types.
     *
     * @param date the date to format
     * @param timezone timezone to use for the formatting (UTC will produce 'Z')
     * @return the date formatted as to be AppSync Scalar type compliant
     */
    public static String format(Date date, TimeZone timezone) {
        Calendar calendar = new GregorianCalendar(timezone, Locale.US);
        calendar.setTime(date);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);

        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);

        String formattedTime = "";
        if (hours != 0 || minutes != 0 || seconds != 0 || millis != 0) {
            if (millis > 0) {
                formattedTime = String.format(Locale.US, "T%02d:%02d:%02d:%03d", hours, minutes, seconds, millis);
            } else if (seconds > 0) {
                formattedTime = String.format(Locale.US, "T%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                formattedTime = String.format(Locale.US, "T%02d:%02d", hours, minutes);
            }
        }

        String formattedZone = "Z";
        int offset = timezone.getRawOffset();
        if (offset != 0) {
            int hOffset = Math.abs((offset / (SECONDS_IN_ONE_HOUR * MILLIS_IN_ONE_SECOND)) / MINUTES_IN_ONE_HOUR);
            int mOffset = Math.abs((offset / (SECONDS_IN_ONE_HOUR * MILLIS_IN_ONE_SECOND)) % MINUTES_IN_ONE_HOUR);
            int sOffset = Math.abs((offset / MILLIS_IN_ONE_SECOND) % SECONDS_IN_ONE_HOUR);
            char indicator = offset < 0 ? '-' : '+';
            if (sOffset > 0) {
                formattedZone = String.format(Locale.US, "%c%02d:%02d:%02d", indicator, hOffset, mOffset, sOffset);
            } else {
                formattedZone = String.format(Locale.US, "%c%02d:%02d", indicator, hOffset, mOffset);
            }
        }

        return formattedDate + formattedTime + formattedZone;
    }

    /**
     * Parse given string into Date if format is compliant with
     * AppSync Scalar AWSDate or AWSDateTime.
     *
     * @param date the date string to parse
     * @param pos The position to start parsing from, updated to where parsing stopped.
     * @return An instance of Date corresponding to parsed string.
     * @throws ParseException if format does not meet requirements
     */
    @SuppressWarnings("MagicNumber") // Named-capturing groups are only available for API 26+
    public static Date parse(String date, ParsePosition pos) throws ParseException {
        int offset = pos.getIndex();

        Matcher dateMatcher = DATE_PATTERN.matcher(date);

        if (!dateMatcher.find() || dateMatcher.start() > 0) {
            String message = "Failed to parse Date in [" + date + "]. ";
            String suggest = "Verify that date matches 'YYYY-MM-DD' pattern.";
            throw new ParseException(message + suggest, offset);
        }
        offset += dateMatcher.end();
        pos.setIndex(offset);

        int year = Integer.parseInt(dateMatcher.group(1));
        int month = Integer.parseInt(dateMatcher.group(2));
        int day = Integer.parseInt(dateMatcher.group(3));
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        int millis = 0;

        String time = date.substring(offset);
        Matcher timeMatcher = TIME_PATTERN.matcher(time);
        if (timeMatcher.find()) {
            if (timeMatcher.start() > 0) {
                String message = "Failed to parse Time in [" + time + "]. ";
                String suggest = "Verify that time matches 'Thh:mm:ss.sss' pattern.";
                throw new ParseException(message + suggest, offset);
            }
            offset += timeMatcher.end();
            pos.setIndex(offset);

            hours = parseInt(timeMatcher.group(1));
            minutes = parseInt(timeMatcher.group(2));
            seconds = parseInt(timeMatcher.group(3));
            millis = parseInt(timeMatcher.group(4));
        }

        // extract timezone
        String zone = date.substring(offset);
        TimeZone timezone = extractTimeZone(zone, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setLenient(false);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);

        return calendar.getTime();
    }

    @SuppressWarnings("MagicNumber") // Named-capturing groups are only available for API 26+
    private static TimeZone extractTimeZone(String zone, ParsePosition pos) throws ParseException {
        int offset = pos.getIndex();

        // Assume default (local) time zone if none is specified
        if (zone.isEmpty()) {
            return TimeZone.getDefault();
        }

        Matcher zoneMatcher = ZONE_PATTERN.matcher(zone);
        if (!zoneMatcher.matches()) {
            String message = "Failed to parse timezone offset in [" + zone + "]. ";
            String suggest = "Verify that timezone offset matches 'Z|±hh:mm:ss' pattern.";
            throw new ParseException(message + suggest, offset);
        }
        offset += zoneMatcher.end();
        pos.setIndex(offset);

        // "Z" match for UTC timezone
        if (zoneMatcher.group().charAt(0) == 'Z') {
            return TIMEZONE_UTC;
        }

        char indicator = zoneMatcher.group(1).charAt(0);
        int hours = parseInt(zoneMatcher.group(2));
        int minutes = parseInt(zoneMatcher.group(3));
        int seconds = parseInt(zoneMatcher.group(4));

        // Standard timezone if there isn't seconds field
        if (seconds == 0) {
            // GMT-hh:mm format
            String timezoneId = String.format(Locale.US, "GMT%c%02d:%02d", indicator, hours, minutes);
            return TimeZone.getTimeZone(timezoneId);
        }

        // Extended timezone for AWS AppSync Scalar support
        long rawOffset = TimeUnit.HOURS.toMillis(hours);
        rawOffset += TimeUnit.MINUTES.toMillis(minutes);
        rawOffset += TimeUnit.SECONDS.toMillis(seconds);
        if (indicator == '-') {
            rawOffset *= -1;
        }

        return new SimpleTimeZone((int) rawOffset, "Extended");
    }

    /**
     * Utility to parse string to integer. 0 if
     * NumberFormatException was thrown.
     *
     * @param integer String to parse to integer.
     * @return Corresponding integer value upon successful parse.
     *         0 otherwise.
     */
    private static int parseInt(String integer) {
        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
