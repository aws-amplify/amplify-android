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
import org.threeten.bp.OffsetTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.util.Date;

/**
 * Represents a valid extended ISO-8601 Time string, with an optional timezone offset.
 * <p>
 * hh:mm:ss.sss±hh:mm:ss
 * OR
 * hh:mm:ss.sss
 * <p>
 * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#appsync-defined-scalars
 */
public final class AWSTime {
    private final LocalTime localTime;
    private final ZoneOffset zoneOffset;

    /**
     * Constructs an {@link AWSTime} from an {@link Date}.
     * This assumed the timezone is UTC. The UTC time is pulled from {@link Date#getTime()}.
     * @param date A Date, in UTC
     */
    public AWSTime(@NonNull Date date) {
        this.zoneOffset = null;
        this.localTime = Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC).toLocalTime();
    }

    /**
     * Constructs an {@link AWSTime}, with a provided date, and a given offset from UTC, expressed in seconds.
     * @param date A {@link Date}
     * @param offsetInSeconds The number of seconds that offsets the provided date from UTC
     */
    public AWSTime(@NonNull Date date, int offsetInSeconds) {
        this.zoneOffset = ZoneOffset.ofTotalSeconds(offsetInSeconds);
        this.localTime = Instant.ofEpochMilli(date.getTime()).atOffset(this.zoneOffset).toLocalTime();
    }

    /**
     * Constructs an {@link AWSTime} from a valid, extended ISO-8601 Time string.
     * @param text A valid, extended ISO-8601 Time string
     */
    public AWSTime(@NonNull String text) {
        LocalTime localTime;
        ZoneOffset zoneOffset;
        try {
            OffsetTime offsetTime = OffsetTime.parse(text, DateTimeFormatter.ISO_OFFSET_TIME);
            localTime = LocalTime.from(offsetTime);
            zoneOffset = ZoneOffset.from(offsetTime);
        } catch (DateTimeParseException exception) {
            // Optional timezone offset not present
            localTime = LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
            zoneOffset = null;
        }
        this.localTime = localTime;
        this.zoneOffset = zoneOffset;
    }

    /**
     * Builds a string-representation of the time, in ISO-8601 extended offset time format.
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
     * Converts AWSTime to java.util.Date.
     * <p>
     * Date is set as January 1, 1970.
     * Timezone offset is set to UTC if not set.
     *
     * @return representation as a java.util.Date.
     */
    @NonNull
    public Date toDate() {
        ZoneOffset zo = zoneOffset != null ? zoneOffset : ZoneOffset.UTC;
        OffsetDateTime oft = OffsetDateTime.of(LocalDate.ofEpochDay(0), localTime, zo);
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
        throw new IllegalStateException("AWSTime instance does not have a timezone offset.");
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AWSTime time = (AWSTime) thatObject;

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
        return "AWSTime{" +
                "localTime=\'" + localTime + "\'" +
                ", zoneOffset=\'" + zoneOffset + "\'" +
                '}';
    }
}
