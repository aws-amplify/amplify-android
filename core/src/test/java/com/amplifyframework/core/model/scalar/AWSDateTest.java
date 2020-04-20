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

public class AWSDateTest {
    @Test
    public void parsesExpectedFormats() {
        for (String value : Arrays.asList(
                "2001-02-03",
                "2001-02-03Z",
                "2001-02-03+01:30",
                "2001-02-03+01:30:15"
        )) {
            assertEquals(value, new AWSDate(value).format());
        }
    }

    @Test
    public void convertsToAndFromDate() {
        Calendar cal = new GregorianCalendar(2001, 2, 3, 0, 0, 0);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        //        int offsetInMillis = cal.getTimeZone().getOffset(cal.getTimeInMillis());
        Date date = cal.getTime();
        AWSDate awsDate = new AWSDate(date);
        assertEquals(date, awsDate.toDate());
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                awsDate.getOffsetTotalSeconds();
            }
        });
    }

    @Test
    public void convertsToAndFromDateWithOffset() {
        Calendar cal = new GregorianCalendar(2001, 2, 3, 0, 0, 0);
        TimeZone timeZone = TimeZone.getTimeZone("PST");
        cal.setTimeZone(timeZone);
        Date date = cal.getTime();
        int offsetInMillis = timeZone.getOffset(date.getTime());
        int offsetInSeconds = offsetInMillis / 1000;
        AWSDate awsDate = new AWSDate(date, offsetInSeconds);
        assertEquals(date, awsDate.toDate());
        assertEquals(offsetInSeconds, awsDate.getOffsetTotalSeconds());
    }
}
