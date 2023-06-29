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

package com.amplifyframework.core.model.temporal;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Namespace for all the Date/Time related types.
 */
public final class Temporal {

    /**
     * This class acts as a namespace and should not be directly instantiated.
     */
    private Temporal() {}

    /**
     * Represents a valid extended ISO-8601 Date string, with an optional timezone offset.
     * <p>
     * YYYY-MM-DD±hh:mm:ss  (ISO_OFFSET_DATE)
     * or
     * YYYY-MM-DD (ISO_LOCAL_DATE)
     * <p>
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#appsync-defined-scalars
     */
    public static final class Date implements Comparable<Date>, Serializable {

        private static final long serialVersionUID = 1L;
        private final LocalDate localDate;
        private final ZoneOffset zoneOffset;

        /**
         * Constructs a new {@link Temporal.Date} from a Java {@link Date}.
         *
         * @param date A Java Date, relative to UTC
         */
        public Date(@NonNull java.util.Date date) {
            this.zoneOffset = null;
            this.localDate = Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toLocalDate();
        }

        /**
         * Construct sa new {@link Temporal.Date} from a Java {@link Date} and a zone offset in seconds.
         *
         * @param date            A date
         * @param offsetInSeconds Count of seconds that offset the date from UTC
         */
        public Date(@NonNull java.util.Date date, int offsetInSeconds) {
            this.zoneOffset = ZoneOffset.ofTotalSeconds(offsetInSeconds);
            this.localDate = Instant.ofEpochMilli(date.getTime()).atOffset(this.zoneOffset).toLocalDate();
        }

        /**
         * Constructs an {@link Temporal.Date from a }valid extended ISO-8601 Date string,
         * with an optional timezone offset.
         *
         * @param text A valid extended ISO-8601 Date string, with an optional timezone offset
         * @throws IllegalArgumentException when text input is not a valid ISO-8601 Date string.
         */
        public Date(@NonNull String text) {
            LocalDate localDate;
            ZoneOffset zoneOffset;
            try {
                OffsetDateTime odt = OffsetDateTime.parse(text, getOffsetDateTimeFormatter());
                localDate = LocalDate.from(odt);
                zoneOffset = ZoneOffset.from(odt);
            } catch (Exception exception) {
                try {
                    // Optional timezone offset not present
                    localDate = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                    zoneOffset = null;
                } catch (Exception dateTimeParseException) {
                    throw new IllegalArgumentException("Failed to create Temporal.Date object from " + text, exception);
                }
            }
            this.localDate = localDate;
            this.zoneOffset = zoneOffset;
        }

        /**
         * Formats the current {@link Temporal.Date}
         * into an extended ISO-8601 Date string, with an optional timezone offset.
         *
         * @return An extended ISO-8601 Date string, with an optional timezone offset
         */
        public String format() {
            if (zoneOffset != null) {
                OffsetDateTime odt = OffsetDateTime.of(localDate, LocalTime.MIDNIGHT, zoneOffset);
                return getOffsetDateTimeFormatter().format(odt);
            } else {
                return DateTimeFormatter.ISO_LOCAL_DATE.format(this.localDate);
            }
        }

        private DateTimeFormatter getOffsetDateTimeFormatter() {
            return new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_OFFSET_DATE)
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .toFormatter();
        }

        /**
         * Converts {@link Temporal.Date} to {@link java.util.Date}.
         * <p>
         * Time is set as start of day (midnight), since this is not represented by Temporal.Date.
         * Timezone offset is set to UTC if not set, since it is optionally represented by Temporal.Date.
         *
         * @return representation as a java.util.Date.
         */
        public java.util.Date toDate() {
            ZoneOffset zoneOffset = this.zoneOffset != null ? this.zoneOffset : ZoneOffset.UTC;
            OffsetDateTime oft = OffsetDateTime.of(localDate, LocalTime.MIDNIGHT, zoneOffset);
            return new java.util.Date(oft.toInstant().toEpochMilli());
        }

        /**
         * Gets the total zone offset in seconds.
         *
         * @return Zone offset in seconds.
         * @throws IllegalStateException if no zoneOffset is set.
         */
        public int getOffsetTotalSeconds() throws IllegalStateException {
            if (zoneOffset != null) {
                return zoneOffset.getTotalSeconds();
            }
            throw new IllegalStateException("Temporal.Date instance does not have a timezone offset.");
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Temporal.Date date = (Temporal.Date) thatObject;

            return ObjectsCompat.equals(localDate, date.localDate) &&
                    ObjectsCompat.equals(zoneOffset, date.zoneOffset);
        }

        @Override
        public int hashCode() {
            int result = localDate.hashCode();
            result = 31 * result + (zoneOffset != null ? zoneOffset.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Temporal.Date{" +
                    "localDate=\'" + localDate + "\'" +
                    ", zoneOffset=\'" + zoneOffset + "\'" +
                    '}';
        }

        @Override
        public int compareTo(Date date) {
            Objects.requireNonNull(date);
            return toDate().compareTo(date.toDate());
        }
    }

    /**
     * Represents a valid extended ISO-8601 DateTime string.  The time zone offset is compulsory.
     * <p>
     * YYYY-MM-DDThh:mm:ss.sssZ  (ISO_OFFSET_DATE_TIME)
     * <p>
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#appsync-defined-scalars
     */
    public static final class DateTime implements Comparable<DateTime>, Serializable {

        private static final long serialVersionUID = 1L;
        private final OffsetDateTime offsetDateTime;

        /**
         * Constructs a new {@link Temporal.DateTime} from a Java {@link java.util.Date}
         * and a zone offset, in seconds.
         *
         * @param date            A date in local time
         * @param offsetInSeconds The offset of the local timezone with respect to GMT, expressed in seconds.
         */
        public DateTime(@NonNull java.util.Date date, int offsetInSeconds) {
            ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(offsetInSeconds);
            this.offsetDateTime = Instant.ofEpochMilli(date.getTime()).atOffset(zoneOffset);
        }

        /**
         * Constructs an {@link Temporal.DateTime} from a valid extended ISO-8601 DateTime string.
         *
         * @param text a valid extended ISO-8601 DateTime string
         * @throws IllegalArgumentException when text input is not a valid ISO-8601 DateTime string.
         */
        public DateTime(@NonNull String text) {
            try {
                this.offsetDateTime = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to create Temporal.DateTime object from " + text, exception);
            }
        }

        /**
         * Formats the {@link Temporal.DateTime} as an extended ISO-8601 DateTime string.
         *
         * @return an extended ISO-8601 DateTime string
         */
        public String format() {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.offsetDateTime);
        }

        /**
         * Gets a {@link java.util.Date} representation of the {@link Temporal.DateTime}.
         *
         * @return A Java {@link java.util.Date} representation of the {@link Temporal.DateTime}
         */
        public java.util.Date toDate() {
            return new java.util.Date(offsetDateTime.toInstant().toEpochMilli());
        }

        /**
         * Gets the total zone offset in seconds.
         *
         * @return Zone offset in seconds.
         */
        public int getOffsetTotalSeconds() {
            return offsetDateTime.getOffset().getTotalSeconds();
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Temporal.DateTime dateTime = (Temporal.DateTime) thatObject;

            return ObjectsCompat.equals(offsetDateTime, dateTime.offsetDateTime);
        }

        @Override
        public int hashCode() {
            return offsetDateTime.hashCode();
        }

        @Override
        public String toString() {
            return "Temporal.DateTime{" +
                    "offsetDateTime=\'" + offsetDateTime + "\'" +
                    '}';
        }

        @Override
        public int compareTo(DateTime dateTime) {
            Objects.requireNonNull(dateTime);
            return toDate().compareTo(dateTime.toDate());
        }
    }

    /**
     * Represents a valid extended ISO-8601 Time string, with an optional timezone offset.
     * <p>
     * hh:mm:ss.sss±hh:mm:ss
     * OR
     * hh:mm:ss.sss
     * <p>
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#appsync-defined-scalars
     */
    public static final class Time implements Comparable<Time>, Serializable {

        private static final long serialVersionUID = 1L;
        private final LocalTime localTime;
        private final ZoneOffset zoneOffset;

        /**
         * Constructs an {@link Temporal.Time} from an {@link java.util.Date}.
         * This assumed the timezone is UTC. The UTC time is pulled from {@link java.util.Date#getTime()}.
         *
         * @param date A Date, in UTC
         */
        public Time(@NonNull java.util.Date date) {
            this.zoneOffset = null;
            this.localTime = Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toLocalTime();
        }

        /**
         * Constructs an {@link Temporal.Time}, with a provided date, and a given offset from UTC,
         * expressed in seconds.
         *
         * @param date            A {@link java.util.Date}
         * @param offsetInSeconds The number of seconds that offsets the provided date from UTC
         */
        public Time(@NonNull java.util.Date date, int offsetInSeconds) {
            this.zoneOffset = ZoneOffset.ofTotalSeconds(offsetInSeconds);
            this.localTime = Instant.ofEpochMilli(date.getTime()).atOffset(this.zoneOffset).toLocalTime();
        }

        /**
         * Constructs an {@link Temporal.Time} from a valid, extended ISO-8601 Time string.
         *
         * @param text A valid, extended ISO-8601 Time string
         * @throws IllegalArgumentException when text input is not a valid ISO-8601 Time string.
         */
        public Time(@NonNull String text) {
            LocalTime localTime;
            ZoneOffset zoneOffset;
            try {
                OffsetTime offsetTime = OffsetTime.parse(text, DateTimeFormatter.ISO_OFFSET_TIME);
                localTime = LocalTime.from(offsetTime);
                zoneOffset = ZoneOffset.from(offsetTime);
            } catch (Exception exception) {
                try {
                    localTime = LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
                    zoneOffset = null;
                } catch (Exception dateTimeParseException) {
                    throw new IllegalArgumentException("Failed to create Temporal.Time object from " + text, exception);
                }
            }
            this.localTime = localTime;
            this.zoneOffset = zoneOffset;
        }

        /**
         * Builds a string-representation of the time, in ISO-8601 extended offset time format.
         *
         * @return A string-representation of the time, in ISO-8601 extended offset time format
         */
        public String format() {
            if (zoneOffset != null) {
                OffsetTime offsetTime = OffsetTime.of(localTime, zoneOffset);
                return DateTimeFormatter.ISO_OFFSET_TIME.format(offsetTime);
            } else {
                return DateTimeFormatter.ISO_LOCAL_TIME.format(this.localTime);
            }
        }

        /**
         * Converts Temporal.Time to java.util.Date.
         * <p>
         * Date is set as January 1, 1970.
         * Timezone offset is set to UTC if not set.
         *
         * @return representation as a java.util.Date.
         */
        @NonNull
        public java.util.Date toDate() {
            ZoneOffset zo = zoneOffset != null ? zoneOffset : ZoneOffset.UTC;
            OffsetDateTime oft = OffsetDateTime.of(LocalDate.ofEpochDay(0), localTime, zo);
            return new java.util.Date(oft.toInstant().toEpochMilli());
        }

        /**
         * Gets the total zone offset in seconds.
         *
         * @return Zone offset in seconds.
         * @throws IllegalStateException if no zoneOffset is set.
         */
        public int getOffsetTotalSeconds() throws IllegalStateException {
            if (zoneOffset != null) {
                return zoneOffset.getTotalSeconds();
            }
            throw new IllegalStateException("Temporal.Time instance does not have a timezone offset.");
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Temporal.Time time = (Temporal.Time) thatObject;

            return ObjectsCompat.equals(localTime, time.localTime) &&
                    ObjectsCompat.equals(zoneOffset, time.zoneOffset);
        }

        @Override
        public int hashCode() {
            int result = localTime.hashCode();
            result = 31 * result + (zoneOffset != null ? zoneOffset.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Temporal.Time{" +
                    "localTime=\'" +
                    "" + localTime + "\'" +
                    ", zoneOffset=\'" + zoneOffset + "\'" +
                    '}';
        }

        @Override
        public int compareTo(Time time) {
            Objects.requireNonNull(time);
            return toDate().compareTo(time.toDate());
        }
    }

    /**
     * The Temporal.Timestamp scalar type represents the number of seconds that have elapsed
     * since 1970-01-01T00:00Z. Timestamps are serialized and deserialized as numbers.
     * Negative values are also accepted and these represent the number of seconds
     * til 1970-01-01T00:00Z.
     */
    public static final class Timestamp implements Comparable<Timestamp>, Serializable {

        private static final long serialVersionUID = 1L;
        private final long secondsSinceEpoch;

        /**
         * Constructs a new Temporal.Timestamp that represents the current system time.
         */
        public Timestamp() {
            this(new java.util.Date());
        }

        /**
         * Constructs a new Temporal.Timestamp, as an amount of time since the UNIX epoch.
         *
         * @param timeSinceEpoch An amount of time that has elapsed since the UNIX epoch,
         *                       for example: 1_588_703_119L seconds. The unit for this value
         *                       must be passed in the second argument.
         * @param timeUnit       The unit in which the first argument is expressed. For example,
         *                       if the first argument is 1_588_703_119L, that would represent the
         *                       number of seconds between the UNIX epoch and
         *                       Tuesday, May 5, 2020 6:25:19 PM in GMT.
         */
        public Timestamp(long timeSinceEpoch, TimeUnit timeUnit) {
            this.secondsSinceEpoch = timeUnit.toSeconds(timeSinceEpoch);
        }

        /**
         * Constructs an Temporal.Timestamp from a Date.
         *
         * @param date A date, that will be interrogated for the current UNIX time;
         *             any sub-second precision contained in the Date will be discarded.
         */
        public Timestamp(@NonNull java.util.Date date) {
            this(date.getTime(), TimeUnit.MILLISECONDS);
        }

        /**
         * Returns a new Timestamp instance that represents the current system time.
         * @return a new Timestamp instance that represents the current system time.
         */
        public static Timestamp now() {
            return new Timestamp();
        }

        /**
         * Gets the number of seconds that have elapsed since the UNIX epoch.
         *
         * @return Seconds since UNIX epoch
         */
        public long getSecondsSinceEpoch() {
            return this.secondsSinceEpoch;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Temporal.Timestamp that = (Temporal.Timestamp) thatObject;

            return secondsSinceEpoch == that.secondsSinceEpoch;
        }

        @Override
        public int hashCode() {
            return (int) (secondsSinceEpoch ^ (secondsSinceEpoch >>> 32));
        }

        @Override
        public String toString() {
            return "Temporal.Timestamp{" +
                    "timestamp=" + secondsSinceEpoch +
                    '}';
        }

        @Override
        public int compareTo(Timestamp timestamp) {
            Objects.requireNonNull(timestamp);
            return Long.compare(getSecondsSinceEpoch(), timestamp.getSecondsSinceEpoch());
        }
    }
}
