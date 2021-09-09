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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link CustomTypeSchema}.
 */
public final class CustomTypeSchemaTest {
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
}
