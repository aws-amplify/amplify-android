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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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
abstract class AWSTemporal implements Comparable<AWSTemporal> {
    @VisibleForTesting static final String UTC = "UTC";
    @VisibleForTesting static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone(UTC);
    @VisibleForTesting static final char UTC_INDICATOR = 'Z';
    @VisibleForTesting static final String EXTENDED_TIMEZONE_ID = "extended-tz";

    private final Calendar calendar;

    AWSTemporal(@NonNull AWSTemporal scalar) {
        this(scalar.getDate(), scalar.getTimeZone());
    }

    AWSTemporal(@NonNull Date date) {
        this(date, UTC_TIMEZONE);
    }

    AWSTemporal(@NonNull Date date,
                @NonNull TimeZone timezone) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(timezone);

        Calendar from = new GregorianCalendar(timezone);
        from.setTime(date);

        Calendar to = new GregorianCalendar(timezone);
        to.setTimeInMillis(0);

        for (Integer field : getCalendarComponentFields()) {
            to.set(field, from.get(field));
        }

        this.calendar = to;
    }

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
    @NonNull
    static String formatTimeZone(@NonNull TimeZone timezone) {
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
     * Parses given string (starting from provided position index)
     * to a timezone object. For timezone offset that matches that
     * of an existing zoneID, a timezone instance with that zoneID
     * is returned. Otherwise, a custom extended timezone instance
     * is returned.
     * @param time String to parse for timezone
     * @param pos Position inside the string to begin parsing from
     * @return Parsed TimeZone object with correct offset
     * @throws ParseException if an error is encountered while
     *         parsing given string
     */
    @NonNull
    @SuppressWarnings("MagicNumber")
    static TimeZone parseTimeZone(
            @NonNull String time,
            @NonNull ParsePosition pos
    ) throws ParseException {
        // No indicator assumes UTC
        if (pos.getIndex() == time.length()) {
            return UTC_TIMEZONE;
        }

        char indicator = time.charAt(pos.getIndex());
        if (!verifyCharAt(time, pos, UTC_INDICATOR, '-', '+')) {
            throw new ParseException("Found unexpected time zone indicator.", pos.getIndex());
        }

        final TimeZone timezone;
        if (indicator == UTC_INDICATOR) {
            // 'Z' means UTC
            timezone = UTC_TIMEZONE;
        } else {
            // '+' or '-' indicates offset
            int hour = parseInt(time, pos, 2);
            int minute = 0;
            int second = 0;

            // minutes and seconds fields are optional
            if (pos.getIndex() < time.length() && verifyCharAt(time, pos, ':')) {
                minute = parseInt(time, pos, 2);
                if (pos.getIndex() < time.length() && verifyCharAt(time, pos, ':')) {
                    second = parseInt(time, pos, 2);
                }
            }

            if (second == 0) {
                // Standard timezone if there isn't seconds field
                // GMT±hh:mm format
                String timezoneId = String.format(Locale.US, "GMT%c%02d:%02d", indicator, hour, minute);
                timezone = TimeZone.getTimeZone(timezoneId);
            } else {
                // Extended timezone for AWS AppSync Scalar support
                long offset = TimeUnit.HOURS.toMillis(hour);
                offset += TimeUnit.MINUTES.toMillis(minute);
                offset += TimeUnit.SECONDS.toMillis(second);
                if (indicator == '-') {
                    offset *= -1;
                }
                timezone = new SimpleTimeZone((int) offset, EXTENDED_TIMEZONE_ID);
            }
        }

        // Make sure that the string being parsed ends here
        if (pos.getIndex() != time.length()) {
            throw new ParseException("Could not parse the remaining string. ", pos.getIndex());
        }

        return timezone;
    }

    /**
     * Utility method to parse int with certain size from a string.
     * It cannot parse a negative value, and it will automatically
     * update the parse position object to point to the index past
     * parsed integer.
     * @param string String to parse
     * @param pos Position to begin parsing from
     * @param size Length of the integer being parsed
     * @return Parsed integer value
     * @throws NumberFormatException if size is less than 0 or if
     *         parsed integer value is less than 0
     */
    protected static int parseInt(
            @NonNull String string,
            @NonNull ParsePosition pos,
            int size
    ) throws NumberFormatException {
        int start = pos.getIndex();
        int end = start + size;
        if (size < 0) {
            throw new NumberFormatException("Size cannot be less than zero.");
        }

        String sub = string.substring(start, end);
        int parsed = Integer.parseInt(sub);

        if (parsed < 0) {
            throw new NumberFormatException("Negative value is not supported.");
        }

        pos.setIndex(end);
        return parsed;
    }

    /**
     * Utility method to verify that the string being parsed contains
     * the expected character at a given index. Upon successful
     * verification, the parse position object will be automatically
     * incremented to point to the next index. A list of characters
     * can be provided to check if the string contains any one of the
     * specified characters in that index.
     * @param string String to verify the presence of a character
     * @param pos Position to check for presence of a character
     * @param matchAtLeastOne List of characters to match at least one
     * @return True if the character at given index matches any one
     *         of the provided characters
     */
    protected static boolean verifyCharAt(
            @NonNull String string,
            @NonNull ParsePosition pos,
            char... matchAtLeastOne
    ) {
        int index = pos.getIndex();
        for (char expected : matchAtLeastOne) {
            if (expected == string.charAt(index)) {
                pos.setIndex(index + 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@link Date} object representing this temporal object.
     * @return the equivalent Date
     */
    @NonNull
    public final Date getDate() {
        return calendar.getTime();
    }

    /**
     * Returns the UNIX Epoch time in milliseconds.
     * @return the UNIX Epoch time
     */
    @NonNull
    public final long getTime() {
        return calendar.getTimeInMillis();
    }

    /**
     * Returns the associated instance of timezone.
     * This timezone could potentially be a custom extended
     * timezone that allows offset with seconds precision.
     * Such timezone will have the ID of
     * {@link AWSTemporal#EXTENDED_TIMEZONE_ID}.
     * @return the associated instance of timezone
     */
    @NonNull
    public final TimeZone getTimeZone() {
        return calendar.getTimeZone();
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
     * Returns static set of Calendar field IDs that
     * are relevant to this instance of AWSTemporal.
     * @return static set of Calendar field IDs
     */
    @NonNull
    abstract Set<Integer> getCalendarComponentFields();

    /**
     * Returns the formatted string representing the
     * temporal instance. The format will be compliant with
     * the specifications of AWS AppSync Scalar types.
     * @return the formatted string
     */
    @Override
    @NonNull
    public abstract String toString();

    /**
     * Compares this object with another and returns true if it
     * is an instance of {@link AWSTemporal} with the same UNIX
     * epoch time as this object.
     * @param obj Object to check for equality
     * @return true if the two objects represent the same time
     */
    @Override
    public final boolean equals(@Nullable Object obj) {
        if (obj instanceof AWSTemporal) {
            AWSTemporal that = (AWSTemporal) obj;
            return ObjectsCompat.equals(this.getTime(), that.getTime());
        }
        return false;
    }

    /**
     * Compares this object with another and returns the difference
     * in time (in milliseconds) represented by each object.
     * @param that The instance of AWSTemporal object to compare
     * @return Positive value if this object represents a more future
     *         moment in time than the object being compared, negative
     *         if past, and zero if equal
     */
    @Override
    public final int compareTo(@NonNull AWSTemporal that) {
        return (int) (this.getTime() - that.getTime());
    }

    /**
     * Return the UNIX epoch time represented by this object.
     * @return the UNIX epoch time represented by this object
     */
    @Override
    public final int hashCode() {
        return (int) this.getTime();
    }
}
