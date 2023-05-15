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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.temporal.GsonTemporalAdapters;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.core.model.types.GsonJavaTypeAdapters;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Tests the {@link SerializedModelAdapter}.
 */
public final class SerializedModelAdapterTest {
    private Gson gson;

    /**
     * Register some models.
     */
    @Before
    public void setup() {
        GsonBuilder builder = new GsonBuilder();
        GsonJavaTypeAdapters.register(builder);
        GsonTemporalAdapters.register(builder);
        SerializedModelAdapter.register(builder);
        SerializedCustomTypeAdapter.register(builder);
        gson = builder.create();
    }

    /**
     * Tests serialization and deserialization of a simple model that contains temporal types.
     * @throws JSONException if Illegal JSON found in JSONAssert call
     */
    @Test
    public void serdeForSerializedModelWithTemporals() throws JSONException {
        Map<String, Object> serializedData = new HashMap<>();
        serializedData.put("id", "970f1b78-8786-4762-a43a-17c70f966684");
        serializedData.put("name", "Special meeting of the minds");
        serializedData.put("date", "2020-11-05Z");
        serializedData.put("dateTime", "2020-11-05T03:44:28Z");
        serializedData.put("time", "03:44:28Z");
        serializedData.put("timestamp", 1604547868);

        // Now, the real stuff. Build a serialized model, that we can serialize.
        SerializedModel serializedModel = SerializedModel.builder()
                .modelSchema(modelSchemaForMeeting())
                .serializedData(serializedData)
                .build();

        String expectedResourcePath = "serde-for-meeting-in-serialized-model.json";
        String expectedJson = Resources.readAsJson(expectedResourcePath).toString(2);
        String actualJson = new JSONObject(gson.toJson(serializedModel)).toString(2);
        JSONAssert.assertEquals(expectedJson, actualJson, true);

        SerializedModel recovered = gson.fromJson(expectedJson, SerializedModel.class);
        Assert.assertEquals(serializedModel, recovered);
    }

    /**
     * Tests serialization and deserialization of a model that contains an association to another
     * model.
     * @throws JSONException On illegal json found by JSONAssert
     * @throws AmplifyException On unable to parse schema
     */
    @Test
    public void serdeForNestedSerializedModels() throws JSONException, AmplifyException {
        Map<String, Object> blogOwnerSerializedData = new HashMap<>();
        blogOwnerSerializedData.put("name", "A responsible blogger");
        blogOwnerSerializedData.put("id", "2cb080ce-bc93-44c6-aa77-f985af311afa");
        SchemaRegistry.instance().register(new HashSet<>(Arrays.asList(BlogOwner.class, Blog.class)));
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(new HashSet<>(Arrays.asList(BlogOwner.class)));
        Map<String, Object> blogSerializedData = new HashMap<>();
        blogSerializedData.put("name", "A fine blog");
        blogSerializedData.put("owner", SerializedModel.builder()
                .modelSchema(schemaRegistry.getModelSchemaForModelClass(BlogOwner.class))
                .serializedData(Collections.singletonMap("id", blogOwnerSerializedData.get("id")))
                .build());
        blogSerializedData.put("id", "3d128fdd-17a8-45ea-a166-44f6712b86f4");
        SerializedModel blogAsSerializedModel = SerializedModel.builder()
                .modelSchema(modelSchemaForBlog())
                .serializedData(blogSerializedData)
                .build();

        String resourcePath = "serde-for-blog-in-serialized-model.json";
        String expectedJson = new JSONObject(Resources.readAsString(resourcePath)).toString(2);
        String actualJson = new JSONObject(gson.toJson(blogAsSerializedModel)).toString(2);

        Assert.assertEquals(expectedJson, actualJson);

        SerializedModel recovered = gson.fromJson(expectedJson, SerializedModel.class);
        Assert.assertEquals(blogAsSerializedModel, recovered);
    }

    /**
     * Tests serialization and deserialization of a model that contains nested custom type.
     *
     * @throws JSONException On illegal json found by JSONAssert
     * @throws AmplifyException On unable to parse schema
     */
    @Test
    public void serdeForNestedCustomTypes() throws JSONException, AmplifyException {
        CustomTypeSchema phoneSchema = customTypeSchemaForPhone();
        CustomTypeSchema contactSchema = customTypeSchemaForContact();
        ModelSchema personSchema = modelSchemaForPerson();

        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register("Person", personSchema);

        Map<String, Object> phoneSerializedData = new HashMap<>();
        phoneSerializedData.put("countryCode", "+1");
        phoneSerializedData.put("number", "41555555555");
        SerializedCustomType phone = SerializedCustomType.builder()
                .serializedData(phoneSerializedData)
                .customTypeSchema(phoneSchema)
                .build();

        Map<String, Object> contactSerializedData = new HashMap<>();
        contactSerializedData.put("email", "test@test.com");
        contactSerializedData.put("phone", phone);
        SerializedCustomType contact = SerializedCustomType.builder()
                .serializedData(contactSerializedData)
                .customTypeSchema(contactSchema)
                .build();

        Map<String, Object> personSerializedData = new HashMap<>();
        personSerializedData.put("fullName", "Tester Test");
        personSerializedData.put("contact", contact);
        personSerializedData.put("id", "some-unique-id");
        SerializedModel person = SerializedModel.builder()
                .modelSchema(personSchema)
                .serializedData(personSerializedData)
                .build();

        String resourcePath = "serialized-model-with-nested-custom-type-se-deserialization.json";
        String expectedJson = new JSONObject(Resources.readAsString(resourcePath)).toString(2);
        String actualJson = new JSONObject(gson.toJson(person)).toString(2);

        Assert.assertEquals(expectedJson, actualJson);

        SerializedModel recovered = gson.fromJson(expectedJson, SerializedModel.class);
        Assert.assertEquals(person, recovered);
    }

    private ModelSchema modelSchemaForMeeting() {
        Map<String, ModelField> fields = new HashMap<>();
        fields.put("id", ModelField.builder()
            .name("id")
            .javaClassForValue(String.class)
            .targetType("ID")
            .isRequired(true)
            .build());
        fields.put("date", ModelField.builder()
            .name("date")
            .javaClassForValue(Temporal.Date.class)
            .targetType("AWSDate")
            .build());
        fields.put("dateTime", ModelField.builder()
            .name("dateTime")
            .javaClassForValue(Temporal.DateTime.class)
            .targetType("AWSDateTime")
            .build());
        fields.put("name", ModelField.builder()
            .name("name")
            .javaClassForValue(String.class)
            .targetType("String")
            .isRequired(true)
            .build());
        fields.put("time", ModelField.builder()
            .name("time")
            .javaClassForValue(Temporal.Time.class)
            .targetType("AWSTime")
            .build());
        fields.put("timestamp", ModelField.builder()
            .name("timestamp")
            .javaClassForValue(Temporal.Timestamp.class)
            .targetType("AWSTimestamp")
            .build());
        return ModelSchema.builder()
            .name("Meeting")
            .pluralName("Meetings")
            .authRules(Collections.emptyList())
            .fields(fields)
            .associations(Collections.emptyMap())
            .indexes(Collections.emptyMap())
            .modelClass(SerializedModel.class)
            .build();
    }

    private ModelSchema modelSchemaForBlog() {
        Map<String, ModelField> fields = new HashMap<>();
        fields.put("id", ModelField.builder()
            .name("id")
            .javaClassForValue(String.class)
            .targetType("ID")
            .isRequired(true)
            .build());
        fields.put("name", ModelField.builder()
            .name("name")
            .javaClassForValue(String.class)
            .targetType("String")
            .isRequired(true)
            .build());
        fields.put("owner", ModelField.builder()
            .name("owner")
            .javaClassForValue(SerializedModel.class)
            .targetType("BlogOwner")
            .isRequired(true)
            .isModel(true)
            .build());
        fields.put("posts", ModelField.builder()
            .name("posts")
            .javaClassForValue(List.class)
            .targetType("Post")
            .isArray(true)
            .isRequired(false)
            .build());

        Map<String, ModelAssociation> associations = new HashMap<>();
        associations.put("owner", ModelAssociation.builder()
            .name("BelongsTo")
            .targetName("blogOwnerId")
            .associatedType("BlogOwner")
            .build());
        associations.put("posts", ModelAssociation.builder()
            .name("HasMany")
            .associatedName("blog")
            .associatedType("Post")
            .build());

        return ModelSchema.builder()
            .name("Blog")
            .pluralName("Blogs")
            .authRules(Collections.emptyList())
            .fields(fields)
            .associations(associations)
            .indexes(Collections.emptyMap())
            .modelClass(SerializedModel.class)
            .build();
    }

    private CustomTypeSchema customTypeSchemaForPhone() {
        Map<String, CustomTypeField> phoneFields = new HashMap<>();
        phoneFields.put("countryCode", CustomTypeField.builder()
                .name("countryCode")
                .targetType("String")
                .javaClassForValue(String.class)
                .isRequired(true)
                .build());
        phoneFields.put("number", CustomTypeField.builder()
                .name("number")
                .targetType("String")
                .javaClassForValue(String.class)
                .isRequired(true)
                .build());
        return CustomTypeSchema.builder()
                .name("Phone")
                .pluralName("Phones")
                .fields(phoneFields)
                .build();
    }

    private CustomTypeSchema customTypeSchemaForContact() {
        Map<String, CustomTypeField> contactFields = new HashMap<>();
        contactFields.put("email", CustomTypeField.builder()
                .name("email")
                .targetType("String")
                .javaClassForValue(String.class)
                .isRequired(true)
                .build());
        contactFields.put("phone", CustomTypeField.builder()
                .name("phone")
                .targetType("Phone")
                .javaClassForValue(Map.class)
                .isCustomType(true)
                .isRequired(true)
                .build());
        return CustomTypeSchema.builder()
                .name("Contact")
                .pluralName("Contacts")
                .fields(contactFields)
                .build();
    }

    private ModelSchema modelSchemaForPerson() {
        Map<String, ModelField> personFields = new HashMap<>();
        personFields.put("id", ModelField.builder()
                .name("id")
                .javaClassForValue(String.class)
                .targetType("ID")
                .isRequired(true)
                .build());
        personFields.put("fullName", ModelField.builder()
                .name("fullName")
                .targetType("String")
                .javaClassForValue(String.class)
                .isRequired(true)
                .build());
        personFields.put("contact", ModelField.builder()
                .name("contact")
                .targetType("Contact")
                .javaClassForValue(Map.class)
                .isRequired(true)
                .isCustomType(true)
                .build());
        return ModelSchema.builder()
                .name("Person")
                .pluralName("People")
                .fields(personFields)
                .build();
    }
}
