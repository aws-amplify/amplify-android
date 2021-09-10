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
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SerializedModeTest {
    private Gson gson;
    private SchemaRegistry schemaRegistry;

    /**
     * Register some schemas and get gson instance.
     */
    @Before
    public void setup() {
        gson = new GsonBuilder().serializeNulls().create();
        schemaRegistry = SchemaRegistry.instance();
        buildSerializedModelNestsSerializedCustomTypeSchemas();
    }

    /**
     * Unregister schemas.
     */
    @After
    public void tearDown() {
        clearSerializedModelNestsSerializedCustomTypeSchemas();
    }

    /**
     * Test {@link SerializedModel#parseSerializedData(Map, String, SchemaRegistry)} can correctly generate
     * serializedData for a {@link SerializedModel} that includes nested {@link SerializedCustomType}.
     */
    @Test
    public void parseSerializedDataGenerateCorrectSerializedData() {
        @SuppressWarnings("unchecked")
        Map<String, Object> serializedData = gson.fromJson(
                Resources.readAsString("serialized-model-nests-custom-type-data.json"), Map.class);

        Map<String, Object> parsedSerializedData = SerializedModel.parseSerializedData(serializedData,
                "Person", schemaRegistry);

        assertEquals(serializedData.size(), parsedSerializedData.size());
        assertEquals(serializedData.get("name"), parsedSerializedData.get("name"));
        assertTrue(parsedSerializedData.get("mailingAddresses") instanceof List);

        @SuppressWarnings("unchecked")
        List<Object> mailingAddresses = (List<Object>) parsedSerializedData.get("mailingAddresses");
        @SuppressWarnings("unchecked")
        List<Object> serializedMailingAddress = (List<Object>) serializedData.get("mailingAddresses");
        assert mailingAddresses != null;
        assert serializedMailingAddress != null;
        List<Object> flatMailingAddresses = new ArrayList<>();
        for (Object mailingAddress : mailingAddresses) {
            assertTrue(mailingAddress instanceof SerializedCustomType);
            flatMailingAddresses.add(((SerializedCustomType) mailingAddress).getSerializedData());
        }

        assertArrayEquals(serializedMailingAddress.toArray(), flatMailingAddresses.toArray());

        assertTrue(parsedSerializedData.get("contact") instanceof SerializedCustomType);
        Map<String, Object> contact =
                ((SerializedCustomType) Objects.requireNonNull(parsedSerializedData.get("contact")))
                        .getSerializedData();
        @SuppressWarnings("unchecked")
        Map<String, Object> serializedContact = (Map<String, Object>) serializedData.get("contact");
        assert serializedContact != null;
        assertEquals(serializedContact.get("email"), contact.get("email"));

        assertTrue(contact.get("phone") instanceof SerializedCustomType);
        Map<String, Object> phone =
                ((SerializedCustomType) Objects.requireNonNull(contact.get("phone"))).getSerializedData();
        @SuppressWarnings("unchecked")
        Map<String, Object> serializedPhone = (Map<String, Object>) serializedContact.get("phone");

        assertEquals(serializedPhone, phone);
    }

    private void buildSerializedModelNestsSerializedCustomTypeSchemas() {
        CustomTypeField addressLine1Field = CustomTypeField.builder()
                .name("line1")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField addressLine2Field = CustomTypeField.builder()
                .name("line2")
                .isRequired(false)
                .targetType("String")
                .build();
        CustomTypeField addressCityField = CustomTypeField.builder()
                .name("city")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField addressStateField = CustomTypeField.builder()
                .name("state")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField addressPostalCodeField = CustomTypeField.builder()
                .name("postalCode")
                .isRequired(true)
                .targetType("String")
                .build();

        Map<String, CustomTypeField> addressFields = new HashMap<>();
        addressFields.put("line1", addressLine1Field);
        addressFields.put("line2", addressLine2Field);
        addressFields.put("city", addressCityField);
        addressFields.put("state", addressStateField);
        addressFields.put("postalCode", addressPostalCodeField);
        CustomTypeSchema addressSchema = CustomTypeSchema.builder()
                .name("Address")
                .pluralName("Addresses")
                .fields(addressFields)
                .build();

        CustomTypeField phoneCountryField = CustomTypeField.builder()
                .name("countryCode")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField phoneAreaField = CustomTypeField.builder()
                .name("areaCode")
                .isRequired(true)
                .targetType("String")
                .build();
        CustomTypeField phoneNumberField = CustomTypeField.builder()
                .name("number")
                .isRequired(true)
                .targetType("String")
                .build();
        Map<String, CustomTypeField> phoneFields = new HashMap<>();
        phoneFields.put("countryCode", phoneCountryField);
        phoneFields.put("areaCode", phoneAreaField);
        phoneFields.put("number", phoneNumberField);
        CustomTypeSchema phoneSchema = CustomTypeSchema.builder()
                .name("Phone")
                .pluralName("Phones")
                .fields(phoneFields)
                .build();

        CustomTypeField contactEmailField = CustomTypeField.builder()
                .name("email")
                .isRequired(false)
                .targetType("String")
                .build();
        CustomTypeField contactPhoneField = CustomTypeField.builder()
                .name("phone")
                .isCustomType(true)
                .targetType("Phone")
                .build();
        Map<String, CustomTypeField> contactFields = new HashMap<>();
        contactFields.put("email", contactEmailField);
        contactFields.put("phone", contactPhoneField);
        CustomTypeSchema contactSchema = CustomTypeSchema.builder()
                .name("Contact")
                .pluralName("Contacts")
                .fields(contactFields)
                .build();

        ModelField personContactField = ModelField.builder()
                .name("contact")
                .targetType("Contact")
                .isCustomType(true)
                .isRequired(true)
                .build();
        ModelField personNameField = ModelField.builder()
                .name("name")
                .targetType("String")
                .isRequired(true)
                .build();
        ModelField personMailingAddressesField = ModelField.builder()
                .name("mailingAddresses")
                .targetType("Address")
                .isCustomType(true)
                .isArray(true)
                .build();
        ModelField personIdField = ModelField.builder()
                .name("id")
                .targetType("String")
                .isRequired(true)
                .build();
        Map<String, ModelField> personFields = new HashMap<>();
        personFields.put("contact", personContactField);
        personFields.put("name", personNameField);
        personFields.put("mailingAddresses", personMailingAddressesField);
        personFields.put("id", personIdField);
        ModelSchema personSchema = ModelSchema.builder()
                .name("Person")
                .pluralName("People")
                .fields(personFields)
                .modelClass(SerializedModel.class)
                .build();

        schemaRegistry.register("Address", addressSchema);
        schemaRegistry.register("Phone", phoneSchema);
        schemaRegistry.register("Contact", contactSchema);
        schemaRegistry.register("Person", personSchema);
    }

    private void clearSerializedModelNestsSerializedCustomTypeSchemas() {
        schemaRegistry.clear();
    }
}
