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

import androidx.annotation.Nullable;

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
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync Defined Scalars</a>
 */
@SuppressWarnings("MagicNumber") // Named-capturing groups are only available for API 26+
public final class AWSDateTimeUtils {
    /**
     * The ID assigned to a custom instance of {@link TimeZone} when
     * there is no standard ID that matches the offset.
     */
    public static final String EXTENDED_TIMEZONE_ID = "Extended";

    private static final String UTC_ID = "UTC";
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone(UTC_ID);
    private static final char UTC_INDICATOR = 'Z';
    private static final char DATE_TIME_SEPARATOR = 'T';

    private static final String DATE_REGEX_EXPRESSION =
            "(?:([0-9]{4})-(1[0-2]|[0][1-9])-(3[01]|[12][0-9]|0[1-9]))";
    private static final String TIME_REGEX_EXPRESSION =
            "(?:(2[0-3]|[01][0-9]):([0-5][0-9])(?::([0-5][0-9])(?:.([0-9]{3}))?)?)";
    private static final String ZONE_REGEX_EXPRESSION =
            "(?:Z|([+-])(2[0-3]|[01][0-9])(?::([0-5][0-9])(?::([0-5][0-9]))?)?)";
    private static final String DATE_TIME_REGEX_EXPRESSION =
            DATE_REGEX_EXPRESSION + DATE_TIME_SEPARATOR + TIME_REGEX_EXPRESSION;

    private static final Pattern DATE_PATTERN = Pattern.compile(DATE_REGEX_EXPRESSION);
    private static final Pattern TIME_PATTERN = Pattern.compile(TIME_REGEX_EXPRESSION);
    private static final Pattern ZONE_PATTERN = Pattern.compile(ZONE_REGEX_EXPRESSION);
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(DATE_TIME_REGEX_EXPRESSION);

    /**
     * Dis-allows instantiation of this class.
     */
    private AWSDateTimeUtils() { }

    /**
     * Formats the timezone offset to a string that follows
     * `Z|±hh:mm:ss` pattern according to AppSync scalar specs.
     *
     *   - `Z` if offset is 0;
     *   - Seconds field is optional and will not be printed if 0.
     *
     * @param timezone timezone instance to format
     * @return formatted timezone string
     */
    static String format(TimeZone timezone) {
        int offset = timezone.getRawOffset();
        if (offset == 0) {
            return Character.toString(UTC_INDICATOR);
        }

        long hOffset = Math.abs(TimeUnit.MILLISECONDS.toHours(offset) % TimeUnit.DAYS.toHours(1));
        long mOffset = Math.abs(TimeUnit.MILLISECONDS.toMinutes(offset) % TimeUnit.HOURS.toMinutes(1));
        long sOffset = Math.abs(TimeUnit.MILLISECONDS.toSeconds(offset) % TimeUnit.MINUTES.toSeconds(1));
        char indicator = offset < 0 ? '-' : '+';
        if (sOffset > 0) {
            return String.format(Locale.US, "%c%02d:%02d:%02d", indicator, hOffset, mOffset, sOffset);
        } else {
            return String.format(Locale.US, "%c%02d:%02d", indicator, hOffset, mOffset);
        }
    }

    /**
     * Parses given string into provided class. Only three
     * types of classes are currently supported:
     *
     *   - {@link AWSDateTime}
     *   - {@link AWSDate}
     *   - {@link AWSTime}
     *
     * @param temporal String representing a temporal class
     * @param pos Index position to begin parsing from
     * @param clazz Specific subclass of {@link AWSTemporal}
     *              to deserialize the string to
     * @return Deserialized temporal instance represented by
     *         the provided string
     * @throws ParseException if the string was not compliant
     *         with the AWS AppSync scalar specifications
     */
    @Nullable
    public static AWSTemporal parse(String temporal, ParsePosition pos, Class<?> clazz) throws ParseException {
        if (AWSDateTime.class.equals(clazz)) {
            return parseDateTime(temporal, pos);
        }
        if (AWSDate.class.equals(clazz)) {
            return parseDate(temporal, pos);
        }
        if (AWSTime.class.equals(clazz)) {
            return parseTime(temporal, pos);
        }
        return null;
    }

    private static AWSDateTime parseDateTime(String temporal, ParsePosition pos) throws ParseException {
        int offset = pos.getIndex();

        String dateTime = temporal.substring(offset);
        Matcher dateTimeMatcher = DATE_TIME_PATTERN.matcher(dateTime);
        if (!dateTimeMatcher.find() || dateTimeMatcher.start() > 0) {
            String message = "Failed to parse DateTime in [" + dateTime + "]. ";
            String suggest = "Verify that date/time matches 'YYYY-MM-DDThh:mm:ss.sss' pattern.";
            throw new ParseException(message + suggest, offset);
        }
        offset += dateTimeMatcher.end();
        pos.setIndex(offset);

        int year = parseInt(dateTimeMatcher.group(1));
        int month = parseInt(dateTimeMatcher.group(2));
        int day = parseInt(dateTimeMatcher.group(3));
        int hours = parseInt(dateTimeMatcher.group(4));
        int minutes = parseInt(dateTimeMatcher.group(5));
        int seconds = parseInt(dateTimeMatcher.group(6));
        int millis = parseInt(dateTimeMatcher.group(7));

        // check if there is more to parse;
        // AppSync scalar for AWSDateTime requires timezone offset
        if (offset >= temporal.length()) {
            String message = "The time zone offset is compulsory for AWSDateTime scalar. ";
            String suggest = "Please specify the offset in the following format: 'Z|±hh:mm:ss'.";
            throw new ParseException(message + suggest, offset);
        }

        // extract timezone
        TimeZone timezone = parseTimeZone(temporal, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setLenient(false);

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);

        return new AWSDateTime(timezone, calendar.getTimeInMillis());
    }

    private static AWSDate parseDate(String temporal, ParsePosition pos) throws ParseException {
        int offset = pos.getIndex();

        String date = temporal.substring(offset);
        Matcher dateMatcher = DATE_PATTERN.matcher(date);
        if (!dateMatcher.find() || dateMatcher.start() > 0) {
            String message = "Failed to parse Date in [" + date + "]. ";
            String suggest = "Verify that date matches 'YYYY-MM-DD' pattern.";
            throw new ParseException(message + suggest, offset);
        }
        offset += dateMatcher.end();
        pos.setIndex(offset);

        int year = parseInt(dateMatcher.group(1));
        int month = parseInt(dateMatcher.group(2));
        int day = parseInt(dateMatcher.group(3));

        // extract timezone
        TimeZone timezone = parseTimeZone(temporal, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setLenient(false);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return new AWSDate(timezone, calendar.getTimeInMillis());
    }

    private static AWSTime parseTime(String temporal, ParsePosition pos) throws ParseException {
        int offset = pos.getIndex();

        String time = temporal.substring(offset);
        Matcher timeMatcher = TIME_PATTERN.matcher(time);
        if (!timeMatcher.find() || timeMatcher.start() > 0) {
            String message = "Failed to parse Time in [" + time + "]. ";
            String suggest = "Verify that time matches 'hh:mm:ss.sss' pattern.";
            throw new ParseException(message + suggest, offset);
        }
        offset += timeMatcher.end();
        pos.setIndex(offset);

        int hours = parseInt(timeMatcher.group(1));
        int minutes = parseInt(timeMatcher.group(2));
        int seconds = parseInt(timeMatcher.group(3));
        int millis = parseInt(timeMatcher.group(4));

        // extract timezone
        TimeZone timezone = parseTimeZone(temporal, pos);

        Calendar calendar = new GregorianCalendar(timezone);
        calendar.setLenient(false);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, millis);

        return new AWSTime(timezone, calendar.getTimeInMillis());
    }

    private static TimeZone parseTimeZone(String temporal, ParsePosition pos) throws ParseException {
        int offset = pos.getIndex();

        String zone = temporal.substring(offset);

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
        if (zoneMatcher.group().charAt(0) == UTC_INDICATOR) {
            return TIMEZONE_UTC;
        }

        char indicator = zoneMatcher.group(1).charAt(0);
        int hours = parseInt(zoneMatcher.group(2));
        int minutes = parseInt(zoneMatcher.group(3));
        int seconds = parseInt(zoneMatcher.group(4));

        // Standard timezone if there isn't seconds field
        if (seconds == 0) {
            // GMT±hh:mm format
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

        return new SimpleTimeZone((int) rawOffset, EXTENDED_TIMEZONE_ID);
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
