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

package com.amplifyframework.api.aws;

import com.amplifyframework.api.aws.internal.AWSDateTypeAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test cases to test that GSON deserialization process can correctly
 * handle AWSDateTime format, which DOES NOT exactly follow the
 * standard ISO-8601 format.
 *
 * The main difference is that AWSDateTime can also support seconds
 * field inside TimeZone offset, which will cause a crash in default
 * deserializer in GSON if present.
 */
@SuppressWarnings("MagicNumber")
public class GsonAWSDateTimeTest {
    /*
     * PST on my local machine at the time of writing this,
     * but it should vary based on the machine.
     */
    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    private static Calendar cal;
    private static Gson gson;

    /**
     * Configure gson to use the correct custom type adapter for Date class.
     */
    @BeforeClass
    public static void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new AWSDateTypeAdapter())
                .create();
    }

    /**
     * Test that a string containing just date can be parsed to Date.
     *
     * Amplify will assume local timezone if none is specified. This
     * means that time (in UNIX epoch) is ACTUALLY being affected by
     * the local timezone of the machine that is parsing the string.
     */
    @Test
    public void testDateWithoutOffset() {
        cal = new GregorianCalendar(DEFAULT_TIME_ZONE);
        resetFields(cal);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        Date date = cal.getTime();

        final String json = "\"2020-01-11\"";
        Date deserialized = gson.fromJson(json, Date.class);
        assertEquals(date, deserialized);

//        String serialized = gson.toJson(date);
//        /*
//         * They cannot be exactly same because date will
//         * pick up local timezone if none is specified.
//         * Local timezone is dependent on the machine
//         * this test is running on.
//         *
//         * Should be "2020-01-11-08:00" if running from PST.
//         */
//        assertEquals(json, serialized);
    }

    /**
     * Test that a string containing date and offset can be parsed
     * to Date object with the same UNIX epoch.
     */
    @Test
    public void testDateWithOffset() {
        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02");
        cal = new GregorianCalendar(gmtPlus2);
        resetFields(cal);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        Date date = cal.getTime();

        final String json = "\"2020-01-11+02:00\"";
        Date deserialized = gson.fromJson(json, Date.class);
        assertEquals(date, deserialized);

//        String serialized = gson.toJson(date);
//        /*
//         * They cannot be exactly same because date will
//         * always serialize to local timezone. Java.util.Date
//         * does NOT remember any timezone; just unix epoch.
//         *
//         * Should be "2020-01-10T14:00-08:00" if running from PST.
//         */
//        assertEquals(json, serialized);
    }

    /**
     * Test that a DateTime string can be parsed to Date.
     *
     * Even though the specs for AppSync scalar requires offset to be
     * present on DateTime entity, Amplify will assume local timezone
     * if none is specified. This means that time (in UNIX epoch) is
     * ACTUALLY being affected by the local timezone of the machine
     * that is parsing the string.
     *
     * This should be avoided by following the AppSync guideline to
     * compulsorily appending timezone offset to AWSDateTime.
     */
    @Test
    public void testDateTimeWithoutOffset() {
        cal = new GregorianCalendar(DEFAULT_TIME_ZONE);
        resetFields(cal);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        Date date = cal.getTime();

        final String json = "\"2020-01-11T12:34:56\"";
        Date deserialized = gson.fromJson(json, Date.class);
        assertEquals(date, deserialized);
    }

    /**
     * Test that DateTime string can correctly parse even with non-
     * zero milliseconds field.
     */
    @Test
    public void testDateTimeWithMilliseconds() {
        TimeZone gmtMinus4 = TimeZone.getTimeZone("GMT-04");
        cal = new GregorianCalendar(gmtMinus4);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        cal.set(Calendar.MILLISECOND, 789);
        Date date = cal.getTime();

        final String json = "\"2020-01-11T12:34:56.789-04:00\"";
        Date deserialized = gson.fromJson(json, Date.class);
        assertEquals(date, deserialized);

//        String serialized = gson.toJson(date);
//        /*
//         * They cannot be exactly same because date will
//         * always serialize to local timezone. Java.util.Date
//         * does NOT remember any timezone; just unix epoch.
//         *
//         * Should be "2020-01-11T08:34:56:789-08:00" if running from PST.
//         */
//        assertEquals(json, serialized);
    }

    /**
     * Test that DateTime string with non-standard offset can still
     * be parsed. The information regarding non-standard offset will
     * be lost after deserialization, but the UNIX epoch value will
     * remain the same.
     */
    @Test
    public void testDateTimeWithOffsetSeconds() {
        // +30 seconds custom timezone
        long offsetInMillis = TimeUnit.SECONDS.toMillis(30);
        TimeZone custom = new SimpleTimeZone((int) offsetInMillis, "custom");
        cal = new GregorianCalendar(custom);
        resetFields(cal);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        Date date = cal.getTime();

        final String json = "\"2020-01-11T12:34:56+00:00:30\"";
        Date deserialized = gson.fromJson(json, Date.class);
        assertEquals(date, deserialized);

//        String serialized = gson.toJson(date);
//        /*
//         * They cannot be exactly same because date will
//         * always serialize to local timezone. Java.util.Date
//         * does NOT remember any timezone; just unix epoch.
//         *
//         * Should be "2020-01-11T04:34:26-08:00" if running from PST.
//         */
//        assertEquals(json, serialized);
    }

    private void resetFields(Calendar calendar) {
        calendar.set(Calendar.YEAR, 0);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
