/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.UserAgent;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SQLiteModelFieldTypeConverterTest {

    /**
     * Reset the user agent before each test.
     */
    @Before
    public void reset() {
        UserAgent.reset();
    }

    /**
     * Test TIME conversion for Android (default platform).
     */
    @Test
    public void testConvertRawValueToTargetTimeAndroid() {
        final String value = "16:00:00.050020000";
        final JavaFieldType fieldType = JavaFieldType.TIME;
        final Gson gson = GsonFactory.instance();
        final String expected = "16:00:00.050020000Z";
        final Object actual = SQLiteModelFieldTypeConverter.convertRawValueToTarget(
                value,
                fieldType,
                gson);
        assertEquals(expected, actual);
    }

    /**
     * Test TIME converter for Flutter.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testConvertRawValueToTargetTimeFlutter() throws AmplifyException {
        UserAgent.configure(Map.of(UserAgent.Platform.FLUTTER, "1.0"));
        final String value = "16:00:00.050020000";
        final JavaFieldType fieldType = JavaFieldType.TIME;
        final Gson gson = GsonFactory.instance();
        final Object actual = SQLiteModelFieldTypeConverter.convertRawValueToTarget(
                value,
                fieldType,
                gson);
        final String expected = "16:00:00.050020000";
        assertEquals(expected, actual);
    }

    /**
     * Test DATE_TIME converter for Flutter.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testConvertRawValueToTargetDateTimeFlutter() throws AmplifyException {
        UserAgent.configure(Map.of(UserAgent.Platform.FLUTTER, "1.0"));
        final String value = "2020-01-01T16:00:00.050020000";
        final JavaFieldType fieldType = JavaFieldType.DATE_TIME;
        final Gson gson = GsonFactory.instance();
        final Object actual = SQLiteModelFieldTypeConverter.convertRawValueToTarget(
                value,
                fieldType,
                gson);
        final String expected = "2020-01-01T16:00:00.050020000";
        assertEquals(expected, actual);
    }
}
