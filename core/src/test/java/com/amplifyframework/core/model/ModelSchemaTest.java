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
import static org.junit.Assert.assertSame;

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
}
