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

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link Temporal.DateTime}.
 */
public final class TemporalDateTimeTest {
    /**
     * An {@link Temporal.DateTime} can be constructed from a range of
     * different string representations. The formatted version of an {@link Temporal.DateTime}
     * is rendered to string in a consistent format.
     */
    @Test
    public void parsesExpectedFormats() {
        List<String> values = Arrays.asList(
                "2001-02-03T01:30:15Z",
                "2001-02-03T01:30:15.444Z",
                "2001-02-03T01:30:15.444+05:30",
                "2001-02-03T01:30:15.444+05:30:15"
        );
        for (String value : values) {
            assertEquals(value, new Temporal.DateTime(value).format());
        }

        // Seconds for time is optional when parsing, but always present in formatted output.
        assertEquals("2001-02-03T01:30:00Z", new Temporal.DateTime("2001-02-03T01:30Z").format());
    }

    /**
     * An {@link Temporal.DateTime} may be constructed from a Java {@link Date}, and
     * converted back to one.
     */
    @Test
    public void convertsToAndFromDateWithOffset() {
        Calendar cal = new GregorianCalendar(2001, 2, 3, 4, 5, 6);
        TimeZone timeZone = TimeZone.getTimeZone("PST");
        cal.setTimeZone(timeZone);
        Date date = cal.getTime();
        int offsetInMillis = timeZone.getOffset(date.getTime());
        int offsetInSeconds = offsetInMillis / 1000;

        Temporal.DateTime temporalDateTime = new Temporal.DateTime(date, offsetInSeconds);
        assertEquals(date, temporalDateTime.toDate());
        assertEquals(offsetInSeconds, temporalDateTime.getOffsetTotalSeconds());
    }

    /**
     * A {@link Temporal.DateTime} implements {@link java.lang.Comparable} correctly.
     */
    @Test
    public void temporalDateTimeIsComparable() {
        Temporal.DateTime sixAmPST = new Temporal.DateTime("2001-03-03T06:00:00.000-08:00:00");
        Temporal.DateTime sevenAmPST = new Temporal.DateTime("2001-03-03T07:00:00.000-08:00:00");
        Temporal.DateTime eightAmPST = new Temporal.DateTime("2001-03-03T08:00:00.000-08:00:00");
        Temporal.DateTime eightAmCST = new Temporal.DateTime("2001-03-03T08:00:00.000-06:00:00");

        // Verify comparison of DateTimes with same TimeZone
        assertEquals(1, sevenAmPST.compareTo(sixAmPST));
        assertEquals(0, sevenAmPST.compareTo(sevenAmPST));
        assertEquals(-1, sevenAmPST.compareTo(eightAmPST));

        // Verify comparison of DateTimes with different TimeZones
        assertEquals(1, sevenAmPST.compareTo(eightAmCST));
    }
}
