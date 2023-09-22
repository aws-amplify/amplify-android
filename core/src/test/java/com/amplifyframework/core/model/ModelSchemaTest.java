/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.ecommerce.Item;
import com.amplifyframework.testmodels.ecommerce.Order;
import com.amplifyframework.testmodels.lazy.Blog;
import com.amplifyframework.testmodels.lazy.Post;
import com.amplifyframework.testmodels.personcar.MaritalStatus;
import com.amplifyframework.testmodels.personcar.Person;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link ModelSchema}.
 */
public final class ModelSchemaTest {
    /**
     * The factory {@link ModelSchema#fromModelClass(Class)} will produce
     * an {@link ModelSchema} that meets our expectations for the {@link Person} model.
     * @throws AmplifyException from model schema parsing
     */
    @Test
    public void modelSchemaIsGeneratedForPersonModel() throws AmplifyException {
        Map<String, ModelField> expectedFields = new HashMap<>();
        expectedFields.put("id", ModelField.builder()
            .targetType("ID")
            .name("id")
            .javaClassForValue(String.class)
            .isRequired(true)
            .build());
        expectedFields.put("first_name", ModelField.builder()
            .targetType("String")
            .name("first_name")
            .javaClassForValue(String.class)
            .isRequired(true)
            .build());
        expectedFields.put("last_name", ModelField.builder()
            .targetType("String")
            .name("last_name")
            .javaClassForValue(String.class)
            .isRequired(true)
            .build());
        expectedFields.put("dob", ModelField.builder()
            .targetType("AWSDate")
            .name("dob")
            .javaClassForValue(Temporal.Date.class)
            .build());
        expectedFields.put("age", ModelField.builder()
            .targetType("Int")
            .name("age")
            .javaClassForValue(Integer.class)
            .build());
        expectedFields.put("relationship", ModelField.builder()
            .name("relationship")
            .javaClassForValue(MaritalStatus.class)
            .targetType("MaritalStatus")
            .isEnum(true)
            .build());
        expectedFields.put("createdAt", ModelField.builder()
            .targetType("AWSDateTime")
            .name("createdAt")
            .javaClassForValue(Temporal.DateTime.class)
            .isReadOnly(true)
            .build());
        expectedFields.put("updatedAt", ModelField.builder()
            .targetType("AWSDateTime")
            .name("updatedAt")
            .javaClassForValue(Temporal.DateTime.class)
            .isReadOnly(true)
            .build());

        ModelIndex expectedModelIndex = ModelIndex.builder()
                .indexName("first_name_and_age_based_index")
                .indexFieldNames(Arrays.asList("first_name", "age"))
                .build();

        ModelSchema expectedModelSchema = ModelSchema.builder()
            .fields(expectedFields)
            .indexes(Collections.singletonMap("first_name_and_age_based_index", expectedModelIndex))
            .name("Person")
            .modelClass(Person.class)
            .build();
        ModelSchema actualModelSchema = ModelSchema.fromModelClass(Person.class);
        assertEquals(expectedModelSchema, actualModelSchema);

        // Sneaking in a cheeky lil' hashCode() test here, while we have two equals()
        // ModelSchema in scope....
        Set<ModelSchema> modelSchemaSet = new HashSet<>();
        modelSchemaSet.add(actualModelSchema);
        modelSchemaSet.add(expectedModelSchema);
        assertEquals(1, modelSchemaSet.size());

        // The object reference is the first one that was put into map
        // (actualModelSchema was first call).
        // The call to add expectedModelSchema was a no-op since hashCode()
        // showed that the object was already in the collection.
        assertSame(actualModelSchema, modelSchemaSet.iterator().next());
    }

    /**
     * The factory {@link ModelSchema#fromModelClass(Class)} will produce
     * an {@link ModelSchema} that meets our expectations for the {@link Post} model.
     * @throws AmplifyException from model schema parsing
     */
    @Test
    public void modelSchemaAllowsLazyTypes() throws AmplifyException {
        Map<String, ModelField> expectedFields = new HashMap<>();

        expectedFields.put("id", ModelField.builder()
                .targetType("ID")
                .name("id")
                .javaClassForValue(String.class)
                .isRequired(true)
                .build());
        expectedFields.put("name", ModelField.builder()
                .targetType("String")
                .name("name")
                .javaClassForValue(String.class)
                .isRequired(true)
                .build());
        expectedFields.put("createdAt", ModelField.builder()
                .targetType("AWSDateTime")
                .name("createdAt")
                .javaClassForValue(Temporal.DateTime.class)
                .isReadOnly(true)
                .build());
        expectedFields.put("updatedAt", ModelField.builder()
                .targetType("AWSDateTime")
                .name("updatedAt")
                .javaClassForValue(Temporal.DateTime.class)
                .isReadOnly(true)
                .build());
        expectedFields.put("blog", ModelField.builder()
                .targetType("Blog")
                .name("blog")
                .javaClassForValue(Blog.class)
                .isRequired(true)
                .isModelReference(true)
                .isModelList(false)
                .build());
        expectedFields.put("comments", ModelField.builder()
                .targetType("Comment")
                .name("comments")
                .javaClassForValue(ModelList.class)
                .isRequired(false)
                .isModelReference(false)
                .isModelList(true)
                .build());

        Map<String, ModelAssociation> expectedAssociations = new HashMap<>();

        expectedAssociations.put("blog", ModelAssociation.builder()
                .name("BelongsTo")
                .targetName("blogPostsId")
                .associatedName("blog")
                .associatedType("Blog")
                .build());
        expectedAssociations.put("comments", ModelAssociation.builder()
                .name("HasMany")
                .targetName(null)
                .associatedName("post")
                .associatedType("Comment")
                .build());

        ModelSchema expectedModelSchema = ModelSchema.builder()
                .fields(expectedFields)
                .name("Post")
                .modelClass(Post.class)
                .pluralName("Posts")
                .associations(expectedAssociations)
                .version(1)
                .build();
        ModelSchema actualModelSchema = ModelSchema.fromModelClass(Post.class);
        assertEquals(expectedModelSchema, actualModelSchema);

        // Sneaking in a cheeky lil' hashCode() test here, while we have two equals()
        // ModelSchema in scope....
        Set<ModelSchema> modelSchemaSet = new HashSet<>();
        modelSchemaSet.add(actualModelSchema);
        modelSchemaSet.add(expectedModelSchema);
        assertEquals(1, modelSchemaSet.size());

        // The object reference is the first one that was put into map
        // (actualModelSchema was first call).
        // The call to add expectedModelSchema was a no-op since hashCode()
        // showed that the object was already in the collection.
        assertSame(actualModelSchema, modelSchemaSet.iterator().next());

        // Double check lazy field reference values are correct
        ModelField blogField = actualModelSchema.getFields().get("blog");
        assertTrue(blogField.isModelReference());
        assertFalse(blogField.isModelList());
        ModelField commentsField = actualModelSchema.getFields().get("comments");
        assertFalse(commentsField.isModelReference());
        assertTrue(commentsField.isModelList());
    }

    /**
     * A model with no @Index annotations should return the default primary index fields. ["id"]
     * @throws AmplifyException from model schema parsing
     */
    @Test
    public void modelWithNoIndexesReturnsDefaultPrimaryIndexFields() throws AmplifyException {
        ModelSchema actualModelSchema = ModelSchema.fromModelClass(BlogOwner.class);
        assertEquals(Arrays.asList("id"), actualModelSchema.getPrimaryIndexFields());
    }

    /**
     * A model with a secondary @Index defined, but no primary @Index defined, should return the default primary
     * index fields, ["id"].
     * @throws AmplifyException from model schema parsing
     */
    @Test
    public void modelWithSecondaryIndexReturnsDefaultPrimaryIndexFields() throws AmplifyException {
        ModelSchema actualModelSchema = ModelSchema.fromModelClass(Person.class);
        assertEquals(Arrays.asList("id"), actualModelSchema.getPrimaryIndexFields());
    }

    /**
     * A model with a single primary key @Index, should return the primary key fields.
     * @throws AmplifyException from model schema parsing
     */
    @Test
    public void modelWithIndexReturnsExpectedPrimaryIndexFields() throws AmplifyException {
        ModelSchema actualModelSchema = ModelSchema.fromModelClass(Order.class);
        assertEquals(Arrays.asList("customerEmail", "createdAt"), actualModelSchema.getPrimaryIndexFields());
    }

    /**
     * A model with a primary and a secondary @Index annotation, should return the primary key fields.  Validates that
     * we parse the @Indexes wrapper correctly, which is only present when the number of @Index annotations is greater
     * than 1.
     * @throws AmplifyException from model schema parsing
     */
    @Test
    public void modelWithIndexesReturnsExpectedPrimaryIndexFields() throws AmplifyException {
        ModelSchema actualModelSchema = ModelSchema.fromModelClass(Item.class);
        assertEquals(Arrays.asList("orderId", "status", "createdAt"), actualModelSchema.getPrimaryIndexFields());
    }
}
