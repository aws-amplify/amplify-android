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

import com.amplifyframework.testutils.Resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GsonUtilTest {

    @Test
    public void validateJsonObjectToMap() {

        // Build the expected Map
        List<Object> innerArray = Arrays.asList(
                "foo",
                0,
                2,
                3.5f,
                true,
                false,
                null);

        Map<String, Object> innerObject = new HashMap<>();
        innerObject.put("someString", "bar");

        List<Object> array = Arrays.asList(
                innerArray,
                innerObject
        );

        Map<String, Object> object = new HashMap<>();
        object.put("someString", "baz");
        object.put("someInteger", 4);
        object.put("someFloat", 5.5f);
        object.put("someBoolean", false);
        object.put("someNull", null);

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("array", array);
        expected.put("object", object);

        // Build the actual Map using GsonUtil
        final String json = Resources.readAsString("gson-util.json");
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Object> actual = GsonUtil.toMap(jsonObject);

        // Assert that the response is expected
        assertEquals(expected, actual);
    }
}
