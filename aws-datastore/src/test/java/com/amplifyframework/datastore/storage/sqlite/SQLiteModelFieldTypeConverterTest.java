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
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.UserAgent;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        setUserAgent();
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
     * Set user agent to Flutter.
     * @throws AmplifyException not expected.
     */
    private void setUserAgent() throws AmplifyException {
        Map<UserAgent.Platform, String> map = new HashMap<>();
        map.put(UserAgent.Platform.FLUTTER, "1.0");
        UserAgent.configure(map);
    }

    /**
     * Test DATE_TIME converter for Flutter.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testConvertRawValueToTargetDateTimeFlutter() throws AmplifyException {
        setUserAgent();
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

    /**
     * Test the convertValueFromTarget getting serialized value from a CustomType field in Flutter user cases.
     * @throws DataStoreException Not expected.
     */
    @Test
    public void testConvertFromCustomTypeTargetValueFlutter() throws DataStoreException {
        SerializedModel model = getTestSerializedModel();
        ModelField testField = ModelField.builder()
                .name("customTypeField")
                .javaClassForValue(Map.class)
                .targetType("DummyCustomType")
                .isRequired(true)
                .isCustomType(true)
                .build();

        SQLiteModelFieldTypeConverter converter = new SQLiteModelFieldTypeConverter(
                ModelSchema.builder().name("DummySchemaNotUsedInTest").modelType(Model.Type.USER).build(),
                SchemaRegistry.instance(),
                new Gson()
        );

        Object result = converter.convertValueFromTarget(model, testField);
        assertEquals(result, "{\"phone\":\"4155555555\",\"countryCode\":\"+1\"}");
    }

    /**
     * Test the convertValueFromTarget getting serialized value from a list of CustomType field in Flutter user cases.
     * @throws DataStoreException Not expected.
     */
    @Test
    public void testConvertFromListOfCustomTypeTargetValuesFlutter() throws DataStoreException {
        SerializedModel model = getTestSerializedModel();
        ModelField testField = ModelField.builder()
                .name("listCustomTypeField")
                .javaClassForValue(List.class)
                .targetType("DummyCustomType")
                .isRequired(true)
                .isCustomType(true)
                .isArray(true)
                .build();

        SQLiteModelFieldTypeConverter converter = new SQLiteModelFieldTypeConverter(
                ModelSchema.builder().name("DummySchemaNotUsedInTest").modelType(Model.Type.USER).build(),
                SchemaRegistry.instance(),
                new Gson()
        );

        Object result = converter.convertValueFromTarget(model, testField);
        assertEquals(result, "[{\"phone\":\"4155555555\",\"countryCode\":\"+1\"},{\"phone\":\"4155555555\"," +
                "\"countryCode\":\"+1\"}]");
    }

    private SerializedModel getTestSerializedModel() {
        Map<String, Object> serializedCustomTypeData = new HashMap<>();
        serializedCustomTypeData.put("phone", "4155555555");
        serializedCustomTypeData.put("countryCode", "+1");

        Map<String, CustomTypeField> customTypeFields = new HashMap<>();
        customTypeFields.put("phone", CustomTypeField.builder()
                .name("phone")
                .targetType("String")
                .javaClassForValue(String.class)
                .build());
        customTypeFields.put("countryCode", CustomTypeField.builder()
                .name("countryCode")
                .targetType("String")
                .javaClassForValue(String.class)
                .build());

        SerializedCustomType testCustomType = SerializedCustomType.builder()
                .serializedData(serializedCustomTypeData)
                .customTypeSchema(CustomTypeSchema.builder()
                        .name("Phone")
                        .pluralName("Phones")
                        .fields(customTypeFields)
                        .build())
                .build();

        List<SerializedCustomType> testCustomTypeList = new ArrayList<>();
        testCustomTypeList.add(testCustomType);
        testCustomTypeList.add(testCustomType);

        Map<String, Object> serializedModelData = new HashMap<>();
        serializedModelData.put("id", "dummy-id");
        serializedModelData.put("customTypeField", testCustomType);
        serializedModelData.put("listCustomTypeField", testCustomTypeList);

        return SerializedModel.builder()
                .modelSchema(ModelSchema.builder()
                        .name("DummySchemaNotUsed")
                        .modelClass(SerializedModel.class)
                        .build())
                .serializedData(serializedModelData)
                .build();
    }
}
