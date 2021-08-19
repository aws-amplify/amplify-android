/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.types.GsonJavaTypeAdapters;
import com.amplifyframework.testutils.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the {@link SerializedCustomTypeAdapter}.
 */
public final class SerializedCustomTypeAdapterTest {
    private Gson gson;

    /**
     * Register some models.
     */
    @Before
    public void setup() {
        GsonBuilder builder = new GsonBuilder().serializeNulls();
        GsonJavaTypeAdapters.register(builder);
        SerializedCustomTypeAdapter.register(builder);
        gson = builder.create();
    }

    /**
     * Test simple SerializedCustomType serialization and deserialization.
     *
     * @throws JSONException On illegal json found by JSONAssert
     */
    @Test
    public void simpleSerializedCustomTypeSerializeSerializationAndDeserialization() throws JSONException {
        Map<String, Object> serializedData = new HashMap<>();
        serializedData.put("line1", "222 Somewhere far");
        serializedData.put("line2", null);
        serializedData.put("state", "CA");
        serializedData.put("postalCode", "123456");

        SerializedCustomType serializedCustomType = SerializedCustomType.builder()
                .serializedData(serializedData)
                .customTypeSchema(null)
                .build();

        String expectedResourcePath = "serialized-custom-type-se-deserialization.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(gson.toJson(serializedCustomType)).toString(2);
        JSONAssert.assertEquals(expectedJson, actualJson, true);

        SerializedCustomType recovered = gson.fromJson(expectedJson, SerializedCustomType.class);
        Assert.assertEquals(serializedCustomType, recovered);
    }

    /**
     * Test Nested SerializedCustomType serialization and deserialization.
     *
     * @throws JSONException On illegal json found by JSONAssert
     */
    @Test
    public void nestedSerializedCustomTypeSerializeSerializationAndDeserialization() throws JSONException {
        Map<String, Object> addressSerializedData1 = new HashMap<>();
        addressSerializedData1.put("line1", "222 Somewhere far");
        addressSerializedData1.put("line2", null);
        addressSerializedData1.put("state", "CA");
        addressSerializedData1.put("postalCode", "123456");

        SerializedCustomType address1 = SerializedCustomType.builder()
                .serializedData(addressSerializedData1)
                .customTypeSchema(null)
                .build();

        Map<String, Object> addressSerializedData2 = new HashMap<>();
        addressSerializedData2.put("line1", "444 Somewhere close");
        addressSerializedData2.put("line2", "Apt 3");
        addressSerializedData2.put("state", "WA");
        addressSerializedData2.put("postalCode", "123456");

        SerializedCustomType address2 = SerializedCustomType.builder()
                .serializedData(addressSerializedData2)
                .customTypeSchema(null)
                .build();

        ArrayList<SerializedCustomType> addresses = new ArrayList<>();
        addresses.add(address1);
        addresses.add(address2);

        Map<String, Object> phoneSerializedData = new HashMap<>();
        phoneSerializedData.put("countryCode", "1");
        phoneSerializedData.put("areaCode", "415");
        phoneSerializedData.put("phone", "6666666");

        SerializedCustomType phone = SerializedCustomType.builder()
                .serializedData(phoneSerializedData)
                .customTypeSchema(null)
                .build();

        Map<String, Object> contactSerializedData = new HashMap<>();
        contactSerializedData.put("email", "tester@testing.com");
        contactSerializedData.put("phone", phone);

        SerializedCustomType contact = SerializedCustomType.builder()
                .serializedData(contactSerializedData)
                .customTypeSchema(null)
                .build();

        ArrayList<String> stringList = new ArrayList<>();
        stringList.add("string1");
        stringList.add("string2");

        Map<String, Object> personalSerializedData = new HashMap<>();
        personalSerializedData.put("name", "Tester Testing");
        personalSerializedData.put("mailingAddress", addresses);
        personalSerializedData.put("contact", contact);
        personalSerializedData.put("arrayList", stringList);

        SerializedCustomType person = SerializedCustomType.builder()
                .serializedData(personalSerializedData)
                .customTypeSchema(null)
                .build();

        String expectedResourcePath = "nested-serialized-custom-type-se-deserialization.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(gson.toJson(person)).toString(2);
        JSONAssert.assertEquals(expectedJson, actualJson, true);

        SerializedCustomType recovered = gson.fromJson(expectedJson, SerializedCustomType.class);
        Assert.assertEquals(person, recovered);
    }

    /**
     * Test Nested SerializedCustomType serialization and deserialization.
     *
     * @throws JSONException On illegal json found by JSONAssert
     */
    @Test
    public void serializedCustomTypeNestsOtherTypes() {
        Map<String, Object> bioSerializedData = new HashMap<>();
        bioSerializedData.put("name", "Someone Testing");
        bioSerializedData.put("birthday", "2020-11-05Z");
        bioSerializedData.put("dateTime", "2020-11-05T03:44:28Z");
        bioSerializedData.put("time", "03:44:28Z");
        bioSerializedData.put("timestamp", 1604547868);
    }
}
