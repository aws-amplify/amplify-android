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

import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
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
import java.util.List;
import java.util.Map;

/**
 * Tests the {@link SerializedCustomTypeAdapter}.
 */
public final class SerializedCustomTypeAdapterTest {
    private Gson gson;
    private CustomTypeSchema addressSchema;
    private CustomTypeSchema contactSchema;
    private CustomTypeSchema phoneSchema;
    private CustomTypeSchema personInfoSchema;

    /**
     * Set up Gson adapters and create testing custom type schema.
     */
    @Before
    public void setup() {
        GsonBuilder builder = new GsonBuilder().serializeNulls();
        GsonJavaTypeAdapters.register(builder);
        SerializedCustomTypeAdapter.register(builder);
        gson = builder.create();

        /*
          # Testing schema:
          # All custom types (non-models)
          type Person {
            name: String!
            mailingAddresses: [Address]
            contact: Contact
            tags: [String]
          }

          type Address {
            line1: String!
            line2: String
            postalCode: String!
            state: String!
          }

          type Phone {
            countryCode: String!
            areaCode: String!
            number: String!
          }

          type Contact {
            phone: Phone
            email: String
          }
         */
        createCustomTypeSchema();
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

        SerializedCustomType serializedAddressCustomType = SerializedCustomType.builder()
                .serializedData(serializedData)
                .customTypeSchema(addressSchema)
                .build();

        String expectedResourcePath = "serialized-custom-type-se-deserialization.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(gson.toJson(serializedAddressCustomType)).toString(2);
        JSONAssert.assertEquals(expectedJson, actualJson, true);

        SerializedCustomType recovered = gson.fromJson(expectedJson, SerializedCustomType.class);
        Assert.assertEquals(serializedAddressCustomType, recovered);
    }

    /**
     * Test Nested SerializedCustomType serialization and deserialization.
     *
     * @throws JSONException On illegal json found by JSONAssert
     */
    @Test
    public void nestedSerializedCustomType() throws JSONException {
        Map<String, Object> addressSerializedData1 = new HashMap<>();
        addressSerializedData1.put("line1", "222 Somewhere far");
        addressSerializedData1.put("line2", null);
        addressSerializedData1.put("state", "CA");
        addressSerializedData1.put("postalCode", "123456");

        SerializedCustomType address1 = SerializedCustomType.builder()
                .serializedData(addressSerializedData1)
                .customTypeSchema(addressSchema)
                .build();

        Map<String, Object> addressSerializedData2 = new HashMap<>();
        addressSerializedData2.put("line1", "444 Somewhere close");
        addressSerializedData2.put("line2", "Apt 3");
        addressSerializedData2.put("state", "WA");
        addressSerializedData2.put("postalCode", "123456");

        SerializedCustomType address2 = SerializedCustomType.builder()
                .serializedData(addressSerializedData2)
                .customTypeSchema(addressSchema)
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
                .customTypeSchema(phoneSchema)
                .build();

        Map<String, Object> contactSerializedData = new HashMap<>();
        contactSerializedData.put("email", "tester@testing.com");
        contactSerializedData.put("phone", phone);

        SerializedCustomType contact = SerializedCustomType.builder()
                .serializedData(contactSerializedData)
                .customTypeSchema(contactSchema)
                .build();

        ArrayList<String> tags = new ArrayList<>();
        tags.add("string1");
        tags.add("string2");

        Map<String, Object> personalSerializedData = new HashMap<>();
        personalSerializedData.put("name", "Tester Testing");
        personalSerializedData.put("mailingAddresses", addresses);
        personalSerializedData.put("contact", contact);
        personalSerializedData.put("tags", tags);

        SerializedCustomType person = SerializedCustomType.builder()
                .serializedData(personalSerializedData)
                .customTypeSchema(personInfoSchema)
                .build();

        String expectedResourcePath = "nested-serialized-custom-type-se-deserialization.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(gson.toJson(person)).toString(2);
        JSONAssert.assertEquals(expectedJson, actualJson, true);

        SerializedCustomType recovered = gson.fromJson(expectedJson, SerializedCustomType.class);
        Assert.assertEquals(person, recovered);
    }

    /**
     * Test Nested SerializedCustomType with nullable field having null value serialization and deserialization.
     *
     * @throws JSONException On illegal json found by JSONAssert
     */
    @Test
    public void nestedSerializedCustomTypeWithNullableField() throws JSONException {
        Map<String, Object> addressSerializedData1 = new HashMap<>();
        addressSerializedData1.put("line1", "222 Somewhere far");
        addressSerializedData1.put("line2", null);
        addressSerializedData1.put("state", "CA");
        addressSerializedData1.put("postalCode", "123456");

        SerializedCustomType address1 = SerializedCustomType.builder()
                .serializedData(addressSerializedData1)
                .customTypeSchema(addressSchema)
                .build();

        Map<String, Object> addressSerializedData2 = new HashMap<>();
        addressSerializedData2.put("line1", "444 Somewhere close");
        addressSerializedData2.put("line2", "Apt 3");
        addressSerializedData2.put("state", "WA");
        addressSerializedData2.put("postalCode", "123456");

        SerializedCustomType address2 = SerializedCustomType.builder()
                .serializedData(addressSerializedData2)
                .customTypeSchema(addressSchema)
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
                .customTypeSchema(phoneSchema)
                .build();

        Map<String, Object> contactSerializedData = new HashMap<>();
        contactSerializedData.put("email", "tester@testing.com");
        contactSerializedData.put("phone", null);

        SerializedCustomType contact = SerializedCustomType.builder()
                .serializedData(contactSerializedData)
                .customTypeSchema(contactSchema)
                .build();

        ArrayList<String> tags = new ArrayList<>();
        tags.add("string1");
        tags.add("string2");

        Map<String, Object> personalSerializedData = new HashMap<>();
        personalSerializedData.put("name", "Tester Testing");
        personalSerializedData.put("mailingAddresses", addresses);
        personalSerializedData.put("contact", contact);
        personalSerializedData.put("tags", tags);

        SerializedCustomType person = SerializedCustomType.builder()
                .serializedData(personalSerializedData)
                .customTypeSchema(personInfoSchema)
                .build();

        String expectedResourcePath = "nested-serialized-custom-type-se-deserialization-null-field.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(gson.toJson(person)).toString(2);
        JSONAssert.assertEquals(expectedJson, actualJson, true);

        SerializedCustomType recovered = gson.fromJson(expectedJson, SerializedCustomType.class);
        Assert.assertEquals(person, recovered);
    }

    private void createCustomTypeSchema() {
        Map<String, CustomTypeField> phoneFields = new HashMap<>();
        phoneFields.put(
                "areaCode", CustomTypeField.builder()
                        .name("areaCode")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        phoneFields.put(
                "number", CustomTypeField.builder()
                        .name("number")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        phoneFields.put(
                "countryCode", CustomTypeField.builder()
                        .name("countryCode")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        phoneSchema = CustomTypeSchema.builder()
                .name("Phone")
                .pluralName("Phones")
                .fields(phoneFields)
                .build();

        Map<String, CustomTypeField> contactFields = new HashMap<>();
        contactFields.put(
                "phone", CustomTypeField.builder()
                        .name("phone")
                        .javaClassForValue(Map.class)
                        .targetType("Phone")
                        .isCustomType(true)
                        .build());
        contactFields.put(
                "email", CustomTypeField.builder()
                        .name("email")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .build());
        contactSchema = CustomTypeSchema.builder()
                .name("Contact")
                .pluralName("Contacts")
                .fields(contactFields)
                .build();

        Map<String, CustomTypeField> addressFields = new HashMap<>();
        addressFields.put(
                "postalCode", CustomTypeField.builder()
                        .name("postalCode")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        addressFields.put(
                "line1", CustomTypeField.builder()
                        .name("line1")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        addressFields.put(
                "line2", CustomTypeField.builder()
                        .name("line2")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .build());
        addressFields.put(
                "state", CustomTypeField.builder()
                        .name("state")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        addressSchema = CustomTypeSchema.builder()
                .name("Address")
                .pluralName("Addresses")
                .fields(addressFields)
                .build();

        Map<String, CustomTypeField> personInfoFields = new HashMap<>();
        personInfoFields.put(
                "name", CustomTypeField.builder()
                        .name("name")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .isRequired(true)
                        .build());
        personInfoFields.put(
                "mailingAddresses", CustomTypeField.builder()
                        .name("mailingAddresses")
                        .javaClassForValue(List.class)
                        .targetType("Address")
                        .isCustomType(true)
                        .isArray(true)
                        .build());
        personInfoFields.put(
                "contact", CustomTypeField.builder()
                        .name("contact")
                        .javaClassForValue(Map.class)
                        .targetType("Contact")
                        .isCustomType(true)
                        .build());
        personInfoFields.put(
                "tags", CustomTypeField.builder()
                        .name("tags")
                        .isArray(true)
                        .javaClassForValue(List.class)
                        .targetType("String")
                        .build());
        personInfoSchema = CustomTypeSchema.builder()
                .name("Person")
                .pluralName("People")
                .fields(personInfoFields)
                .build();
    }
}
