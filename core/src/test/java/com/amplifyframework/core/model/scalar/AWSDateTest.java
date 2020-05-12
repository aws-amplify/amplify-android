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

import com.amplifyframework.core.model.AWSDate;

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
 * Tests the {@link AWSDate}.
 */
public final class AWSDateTest {
    /**
     * An {@link AWSDate} may be created from a variety of different formats of String.
     */
    @Test
    public void parsesExpectedFormats() {
        List<String> values = Arrays.asList(
                "2001-02-03",
                "2001-02-03Z",
                "2001-02-03+01:30",
                "2001-02-03+01:30:15"
        );
        for (String value : values) {
            assertEquals(value, new AWSDate(value).format());
        }
    }

    /**
     * An {@link AWSDate} may be created from a Java {@link Date}, and can
     * be converted back to a Java {@link Date}.
     */
    @Test
    public void convertsToAndFromDate() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0); // clear
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.YEAR, 2001);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DAY_OF_MONTH, 3); // March 3, 2001
        cal.set(Calendar.HOUR_OF_DAY, 0); // 12 AM
        cal.set(Calendar.MINUTE, 0); // 12:00 AM
        cal.set(Calendar.SECOND, 0); // 12:00:00 AM
        Date date = cal.getTime();
        AWSDate awsDate = new AWSDate(date);
        assertEquals(date, awsDate.toDate());
        assertThrows(IllegalStateException.class, awsDate::getOffsetTotalSeconds);
    }

    /**
     * An {@link AWSDate} can be constructed from a Java {@link Date}, along
     * with a timezone offset in seconds.
     */
    @Test
    public void convertsToAndFromDateWithOffset() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0); // clear
        TimeZone timeZone = TimeZone.getTimeZone("PST");
        cal.setTimeZone(timeZone);
        cal.set(Calendar.YEAR, 2001);
        cal.set(Calendar.MONTH, Calendar.MARCH); // 0 is the first month, not 1
        cal.set(Calendar.DAY_OF_MONTH, 3); // 1 is the first day, not 0
        cal.set(Calendar.HOUR_OF_DAY, 0); // 12 AM
        cal.set(Calendar.MINUTE, 0); // 12:00 AM
        cal.set(Calendar.SECOND, 0); // 12:00:00 AM
        Date date = cal.getTime();
        long offsetInMillis = timeZone.getOffset(date.getTime());
        int offsetInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(offsetInMillis);
        AWSDate awsDate = new AWSDate(date, offsetInSeconds);
        assertEquals(date, awsDate.toDate());
        assertEquals(offsetInSeconds, awsDate.getOffsetTotalSeconds());
    }
}
