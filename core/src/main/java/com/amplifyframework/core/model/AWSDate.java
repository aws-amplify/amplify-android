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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.ChronoField;

import java.util.Date;

/**
 * Represents a valid extended ISO-8601 Date string, with an optional timezone offset.
 * <p>
 * YYYY-MM-DD±hh:mm:ss  (ISO_OFFSET_DATE)
 * or
 * YYYY-MM-DD (ISO_LOCAL_DATE)
 * <p>
 * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#appsync-defined-scalars
 */
public final class AWSDate {
    private final LocalDate localDate;
    private final ZoneOffset zoneOffset;

    public AWSDate(@NonNull Date date) {
        this.zoneOffset = null;
        this.localDate = Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toLocalDate();
    }

    public AWSDate(@NonNull Date date, int offsetInSeconds) {
        this.zoneOffset = ZoneOffset.ofTotalSeconds(offsetInSeconds);
        this.localDate = Instant.ofEpochMilli(date.getTime()).atOffset(this.zoneOffset).toLocalDate();
    }

    public AWSDate(@NonNull String text) {
        LocalDate localDate;
        ZoneOffset zoneOffset;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(text, getOffsetDateTimeFormatter());
            localDate = LocalDate.from(odt);
            zoneOffset = ZoneOffset.from(odt);
        } catch (DateTimeParseException exception) {
            // Optional timezone offset not present
            localDate = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            zoneOffset = null;
        }
        this.localDate = localDate;
        this.zoneOffset = zoneOffset;
    }

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
     * Converts AWSDate to java.util.Date.
     * <p>
     * Time is set as start of day (midnight), since this is not represented by AWSDate.
     * Timezone offset is set to UTC if not set, since it is optionally represented by AWSDate.
     *
     * @return representation as a java.util.Date.
     */
    public Date toDate() {
        ZoneOffset zoneOffset = this.zoneOffset != null ? this.zoneOffset : ZoneOffset.UTC;
        OffsetDateTime oft = OffsetDateTime.of(localDate, LocalTime.MIDNIGHT, zoneOffset);
        return DateTimeUtils.toDate(oft.toInstant());
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
        throw new IllegalStateException("AWSDate instance does not have a timezone offset.");
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AWSDate date = (AWSDate) thatObject;

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
        return "AWSDate{" +
                "localDate=\'" + localDate + "\'" +
                ", zoneOffset=\'" + zoneOffset + "\'" +
                '}';
    }
}
