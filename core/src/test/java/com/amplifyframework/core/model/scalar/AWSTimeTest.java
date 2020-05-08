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

import com.amplifyframework.core.model.AWSTime;

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

public class AWSTimeTest {
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
            assertEquals(value, new AWSTime(value).format());
        }

        // Seconds for time is optional when parsing, but always present in formatted output.
        assertEquals("01:22:00", new AWSTime("01:22").format());
    }

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
        Date date = cal.getTime();
        AWSTime awsTime = new AWSTime(date);
        assertEquals(date, awsTime.toDate());
        assertThrows(IllegalStateException.class, () -> awsTime.getOffsetTotalSeconds());
    }

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

        Date date = cal.getTime();
        long offsetInMillis = timeZone.getOffset(date.getTime());
        int offsetInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(offsetInMillis);
        AWSTime awsTime = new AWSTime(date, offsetInSeconds);
        assertEquals(date, awsTime.toDate());
        assertEquals(offsetInSeconds, awsTime.getOffsetTotalSeconds());
    }
}
