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

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AWSTimeTest {
    @Test
    public void parsesExpectedFormats() {
        for (String value : Arrays.asList(
                "01:22:33",
                "01:22:33.444",
                "01:22:33.444Z",
                "01:22:33.444+05:30",
                "01:22:33.444+05:30:15"
        )) {
            assertEquals(value, new AWSTime(value).format());
        }

        // Seconds for time is optional when parsing, but always present in formatted output.
        assertEquals("01:22:00", new AWSTime("01:22").format());
    }

    @Test
    public void convertsToAndFromDate() {
        Calendar cal = new GregorianCalendar(1970, 0, 1, 2, 3, 4);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = cal.getTime();
        AWSTime awsTime = new AWSTime(date);
        assertEquals(date, awsTime.toDate());
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                awsTime.getOffsetTotalSeconds();
            }
        });
    }

    @Test
    public void convertsToAndFromDateWithOffset() {
        Calendar cal = new GregorianCalendar(1970, 0, 1, 2, 3, 4);
        TimeZone timeZone = TimeZone.getTimeZone("PST");
        cal.setTimeZone(timeZone);
        Date date = cal.getTime();
        int offsetInMillis = timeZone.getOffset(date.getTime());
        int offsetInSeconds = offsetInMillis / 1000;
        AWSTime awsTime = new AWSTime(date, offsetInSeconds);
        assertEquals(date, awsTime.toDate());
        assertEquals(offsetInSeconds, awsTime.getOffsetTotalSeconds());
    }
}
