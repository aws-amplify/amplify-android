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

package com.amplifyframework.datastore.storage;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIndex;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.syncengine.PendingMutation;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link PendingMutation.PersistentRecord}, which is a {@link Model}
 * that can be used by the DataStore's {@link LocalStorageAdapter}.
 */
public class PendingMutationPersistentRecordTest {
    /**
     * Generation of a {@link ModelSchema} for the {@link PendingMutation.PersistentRecord}
     * succeeds.
     * @throws AmplifyException from Amplify configuration
     */
    @Test
    public void modelSchemaGenerationSucceeds() throws AmplifyException {
        List<ModelField> fields = Arrays.asList(
            ModelField.builder()
                .name("id")
                .targetType("ID")
                .isRequired(true)
                .javaClassForValue(String.class)
                .build(),
            ModelField.builder()
                .name("containedModelId")
                .targetType("String")
                .isRequired(true)
                .javaClassForValue(String.class)
                .build(),
            ModelField.builder()
                .name("serializedMutationData")
                .targetType("String")
                .isRequired(true)
                .javaClassForValue(String.class)
                .build(),
            ModelField.builder()
                .name("containedModelClassName")
                .targetType("String")
                .isRequired(true)
                .javaClassForValue(String.class)
                .build()
        );

        Map<String, ModelField> expectedFieldsMap = new HashMap<>();
        for (ModelField field : fields) {
            expectedFieldsMap.put(field.getName(), field);
        }

        final ModelIndex index = ModelIndex.builder()
            .indexFieldNames(Collections.singletonList("containedModelClassName"))
            .indexName("containedModelClassNameBasedIndex")
            .build();

        assertEquals(
            // Expected
            ModelSchema.builder()
                .name("PersistentRecord")
                .pluralName("PersistentRecords")
                .fields(expectedFieldsMap)
                .indexes(Collections.singletonMap("containedModelClassNameBasedIndex", index))
                .modelClass(PendingMutation.PersistentRecord.class)
                .build(),
            // Actual
            ModelSchema.fromModelClass(PendingMutation.PersistentRecord.class)
        );
    }
}
