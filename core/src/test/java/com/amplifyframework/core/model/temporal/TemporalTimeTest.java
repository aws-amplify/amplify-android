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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests the {@link Temporal.Time}.
 */
public final class TemporalTimeTest {
    /**
     * There are several possible formats of String for which a valid {@link Temporal.Time}
     * can be constructed.
     */
    @Test
    public void parsesExpectedFormats() {
        List<String> values = Arrays.asList(
                "01:22:33",
                "01:22:33.444",
                "01:22:33.444Z",
                "01:22:33.444+05:30",
                "01:22:33.444+05:30:15"
        );
        for (String value : values) {
            assertEquals(value, new Temporal.Time(value).format());
        }

        // Seconds for time is optional when parsing, but always present in formatted output.
        assertEquals("01:22:00", new Temporal.Time("01:22").format());
    }

    /**
     * An {@link Temporal.Time} may be converted to and from a Java {@link Date}.
     * When no zone offset is provided, the Date is assumed to be relative to GMT.
     */
    @Test
    public void convertsToAndFromDate() {
        Calendar cal = new GregorianCalendar();
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        cal.setTimeZone(timeZone);
        cal.set(Calendar.YEAR, 1970);
        cal.set(Calendar.MONTH, Calendar.JANUARY); // 0 is the first month, not 1
        cal.set(Calendar.DAY_OF_MONTH, 1); // 1 is the first day, not 0
        cal.set(Calendar.HOUR_OF_DAY, 1); // 1 AM
        cal.set(Calendar.MINUTE, 2); // 1:02 AM
        cal.set(Calendar.SECOND, 3); // 1:02:03 AM
        cal.set(Calendar.MILLISECOND, 4); // 1:02:03.004 AM

        Date date = cal.getTime();
        Temporal.Time temporalTime = new Temporal.Time(date);
        assertEquals(date, temporalTime.toDate());
        assertThrows(IllegalStateException.class, temporalTime::getOffsetTotalSeconds);
    }

    /**
     * An {@link Temporal.Time} may be converted to and from a Java {@link Date}.
     * When an offset time is additionally provided, it may be stored into the {@link Temporal.Time}
     * and used for further computations.
     */
    @Test
    public void convertsToAndFromDateWithOffset() {
        Calendar cal = new GregorianCalendar();
        TimeZone timeZone = TimeZone.getTimeZone("PST");
        cal.setTimeZone(timeZone);
        cal.set(Calendar.YEAR, 1970);
        cal.set(Calendar.MONTH, Calendar.JANUARY); // 0 is the first month, not 1
        cal.set(Calendar.DAY_OF_MONTH, 1); // 1 is the first day, not 0
        cal.set(Calendar.HOUR_OF_DAY, 1); // 1 AM
        cal.set(Calendar.MINUTE, 2); // 1:02 AM
        cal.set(Calendar.SECOND, 3); // 1:02:03 AM
        cal.set(Calendar.MILLISECOND, 4); // 1:02:03.004 AM

        Date date = cal.getTime();
        long offsetInMillis = timeZone.getOffset(date.getTime());
        int offsetInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(offsetInMillis);
        Temporal.Time temporalTime = new Temporal.Time(date, offsetInSeconds);
        assertEquals(date, temporalTime.toDate());
        assertEquals(offsetInSeconds, temporalTime.getOffsetTotalSeconds());
    }
}
