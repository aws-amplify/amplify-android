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

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link StorageItemChangeRecord}, which is a {@link Model}
 * that can be used by the DataStore's LocalStorageAdapter, and by the
 * API category.
 */
public class StorageItemChangeRecordTest {
    /**
     * Generation of a ModelSchema for the {@link StorageItemChangeRecord}
     * succeeds.
     * @throws AmplifyException from Amplify configuration
     */
    @Test
    public void modelSchemaGenerationSucceeds() throws AmplifyException {
        Map<String, ModelField> expectedFields = new HashMap<>();
        expectedFields.put("id", ModelField.builder()
            .name("id")
            .targetType("ID")
            .isRequired(true)
            .type(String.class)
            .build());
        expectedFields.put("entry", ModelField.builder()
            .name("entry")
            .targetType("String")
            .isRequired(true)
            .type(String.class)
            .build());
        expectedFields.put("itemClass", ModelField.builder()
            .name("itemClass")
            .targetType("String")
            .isRequired(true)
            .type(String.class)
            .build());

        final ModelIndex index = ModelIndex.builder()
                .indexFieldNames(Collections.singletonList("itemClass"))
                .indexName("itemClassBasedIndex")
                .build();

        assertEquals(
            // Expected
            ModelSchema.builder()
                .name("StorageItemChangeRecord")
                .pluralName("StorageItemChangeRecords")
                .fields(expectedFields)
                .indexes(Collections.singletonMap("itemClassBasedIndex", index))
                .build(),
            // Actual
            ModelSchema.fromModelClass(StorageItemChangeRecord.class)
        );
    }
}
