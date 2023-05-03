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

package com.amplifyframework.core.model;

import com.amplifyframework.testutils.Resources;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link CustomTypeSchema}.
 */
public final class CustomTypeSchemaTest {
    private CustomTypeSchema addressSchema;
    private CustomTypeSchema contactSchema;
    private CustomTypeSchema phoneSchema;
    private CustomTypeSchema personInfoSchema;

    /**
     * Create custom type schema for testing.
     */
    @Before
    public void setup() {
        Map<String, CustomTypeField> phoneFields = new HashMap<>();
        phoneFields.put(
                "areaCode", CustomTypeField.builder()
                        .name("areaCode")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .build());
        phoneFields.put(
                "number", CustomTypeField.builder()
                        .name("number")
                        .javaClassForValue(String.class)
                        .targetType("String")
                        .build());
        phoneFields.put(
                "countryCode", CustomTypeField.builder()
                        .name("countryCode")
                        .javaClassForValue(String.class)
                        .targetType("String")
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
                        .build());
        addressFields.put(
                "line1", CustomTypeField.builder()
                        .name("line1")
                        .javaClassForValue(String.class)
                        .targetType("String")
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

    /**
     * The {@link CustomTypeSchema#builder()} should create expected CustomType schema.
     */
    @Test
    public void customTypeSchemaIsCreated() {
        final String typeName = "Address";
        final String typePluralName = "Addresses";
        final String line1FieldName = "line1";
        final String line2FieldName = "line2";
        final String cityFieldName = "city";
        final String stateFieldName = "state";
        final String postalCodeFieldName = "postalCode";

        CustomTypeField line1Field = CustomTypeField.builder()
                .name(line1FieldName)
                .javaClassForValue(String.class)
                .targetType("String")
                .build();
        CustomTypeField line2Field = CustomTypeField.builder()
                .name(line2FieldName)
                .javaClassForValue(String.class)
                .targetType("String")
                .build();
        CustomTypeField cityField = CustomTypeField.builder()
                .name(cityFieldName)
                .javaClassForValue(String.class)
                .targetType("String")
                .build();
        CustomTypeField stateField = CustomTypeField.builder()
                .name(stateFieldName)
                .javaClassForValue(String.class)
                .targetType("String")
                .build();
        CustomTypeField postalCodeField = CustomTypeField.builder()
                .name(postalCodeFieldName)
                .javaClassForValue(String.class)
                .targetType("String")
                .build();

        Map<String, CustomTypeField> fields = new HashMap<>();
        fields.put(line1FieldName, line1Field);
        fields.put(line2FieldName, line2Field);
        fields.put(cityFieldName, cityField);
        fields.put(stateFieldName, stateField);
        fields.put(postalCodeFieldName, postalCodeField);

        CustomTypeSchema schema = CustomTypeSchema.builder()
                .name(typeName)
                .pluralName(typePluralName)
                .fields(fields)
                .build();

        assertEquals(typeName, schema.getName());
        assertEquals(typePluralName, schema.getPluralName());
        assertEquals(fields, schema.getFields());
    }

    /**
     * The {@link SerializedCustomType#getFlatSerializedData()} should create a map of values.
     *
     *  @throws JSONException On illegal json found by JSONAssert.
     */
    @Test
    public void customTypeGetFlatSerializedData() throws JSONException {
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

        String expectedResourcePath = "serialized-custom-type-nested-data.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(new Gson().toJson(person.getFlatSerializedData())).toString(2);
        assertEquals(expectedJson, actualJson);
    }
}
