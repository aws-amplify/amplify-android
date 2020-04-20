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

package com.amplifyframework.core.model.scalar;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Date;

/**
 * Represents a valid extended ISO-8601 DateTime string.  The time zone offset is compulsory.
 * <p>
 * YYYY-MM-DDThh:mm:ss.sssZ  (ISO_OFFSET_DATE_TIME)
 * <p>
 * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#appsync-defined-scalars
 */
public final class AWSDateTime {
    private final OffsetDateTime offsetDateTime;

    public AWSDateTime(@NonNull Date date, int offsetInSeconds) {
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(offsetInSeconds);
        this.offsetDateTime = Instant.ofEpochMilli(date.getTime()).atOffset(zoneOffset);
    }

    public AWSDateTime(@NonNull String text) {
        this.offsetDateTime = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public String format() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.offsetDateTime);
    }

    public Date toDate() {
        return DateTimeUtils.toDate(offsetDateTime.toInstant());
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

        AWSDateTime dateTime = (AWSDateTime) thatObject;

        return ObjectsCompat.equals(offsetDateTime, dateTime.offsetDateTime);
    }

    @Override
    public int hashCode() {
        return offsetDateTime.hashCode();
    }

    @Override
    public String toString() {
        return "AWSDateTime{" +
                "offsetDateTime=\'" + offsetDateTime + "\'" +
                '}';
    }
}
