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

import com.amplifyframework.api.aws.scalar.AWSDate;
import com.amplifyframework.api.aws.scalar.AWSDateTime;
import com.amplifyframework.api.aws.scalar.AWSTemporalTypeAdapter;
import com.amplifyframework.api.aws.scalar.AWSTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
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
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new AWSTemporalTypeAdapter())
                .create();
    }

    /**
     * Test that a string containing just date can be parsed correctly.
     *
     * Amplify will assume local timezone if none is specified. This
     * means that time (in UNIX epoch) is ACTUALLY being affected by
     * the local timezone of the machine that is parsing the string.
     *
     * This means that the actual value of time being stored (Unix Epoch)
     * will be changed upon deserialization by local timezone offset.
     */
    @Test
    public void testDateWithoutOffset() {
        cal = new GregorianCalendar(DEFAULT_TIME_ZONE);
        cal.setTimeInMillis(0);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        AWSDate expected = new AWSDate(cal.getTimeZone(), cal.getTimeInMillis());

        final String json = "\"2020-01-11\"";
        AWSDate deserialized = gson.fromJson(json, AWSDate.class);
        assertEquals(expected, deserialized);

//        String serialized = gson.toJson(expected);
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
     * to AWSDate object with the same date.
     */
    @Test
    public void testDateWithOffset() {
        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02");
        cal = new GregorianCalendar(gmtPlus2);
        cal.setTimeInMillis(0);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        AWSDate expected = new AWSDate(cal.getTimeZone(), cal.getTimeInMillis());

        final String json = "\"2020-01-11+02:00\"";
        AWSDate deserialized = gson.fromJson(json, AWSDate.class);
        assertEquals(expected, deserialized);

        String serialized = gson.toJson(expected);
        assertEquals(json, serialized);
    }

    /**
     * Test that a AWSDateTime string cannot be parsed without a valid
     * timezone offset. AppSync specifies timezone offset to be
     * compulsory in AWSDateTime scalar.
     */
    @Test(expected = JsonSyntaxException.class)
    public void testDateTimeWithoutOffset() {
        final String json = "\"2020-01-11T12:34:56\"";
        gson.fromJson(json, AWSDateTime.class);
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
        AWSDateTime expected = new AWSDateTime(cal.getTimeZone(), cal.getTimeInMillis());

        final String json = "\"2020-01-11T12:34:56.789-04:00\"";
        AWSDateTime deserialized = gson.fromJson(json, AWSDateTime.class);
        assertEquals(expected, deserialized);

        String serialized = gson.toJson(expected);
        assertEquals(json, serialized);
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
        cal.setTimeInMillis(0);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        AWSDateTime expected = new AWSDateTime(cal.getTimeZone(), cal.getTimeInMillis());

        final String json = "\"2020-01-11T12:34:56+00:00:30\"";
        AWSDateTime deserialized = gson.fromJson(json, AWSDateTime.class);
        assertEquals(expected, deserialized);

        String serialized = gson.toJson(expected);
        assertEquals(json, serialized);
    }

    /**
     * Test that a string containing just time can be parsed correctly.
     *
     * Amplify will assume local timezone if none is specified. This
     * means that time (in UNIX epoch) is ACTUALLY being affected by
     * the local timezone of the machine that is parsing the string.
     *
     * This means that the actual value of time being stored (Unix Epoch)
     * will be changed upon deserialization by local timezone offset.
     */
    @Test
    public void testTimeWithoutOffset() {
        cal = new GregorianCalendar(DEFAULT_TIME_ZONE);
        cal.setTimeInMillis(0);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        AWSTime expected = new AWSTime(cal.getTimeZone(), cal.getTimeInMillis());

        final String json = "\"12:34:56\"";
        AWSTime deserialized = gson.fromJson(json, AWSTime.class);
        assertEquals(expected, deserialized);

//        String serialized = gson.toJson(expected);
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
     * Test that a string containing time and offset can be parsed
     * to AWSTime object with the same time of the day.
     */
    @Test
    public void testTimeWithOffset() {
        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02");
        cal = new GregorianCalendar(gmtPlus2);
        cal.setTimeInMillis(0);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        AWSTime expected = new AWSTime(cal.getTimeZone(), cal.getTimeInMillis());

        final String json = "\"12:34:56+02:00\"";
        AWSTime deserialized = gson.fromJson(json, AWSTime.class);
        assertEquals(expected, deserialized);

        String serialized = gson.toJson(expected);
        assertEquals(json, serialized);
    }
}
