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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the functionality of the {@link GsonPendingMutationConverter}.
 */
public final class GsonPendingMutationConverterTest {
    /**
     * Validate that the {@link GsonPendingMutationConverter} can be
     * used to convert a sample {@link PendingMutation} to a
     * {@link PendingMutation.PersistentRecord}, and vice-versa.
     * @throws DataStoreException from DataStore conversion
     * @throws AmplifyException On failure to arrange model schema
     */
    @Test
    public void convertStorageItemChangeToRecordAndBack() throws AmplifyException {
        // Arrange a PendingMutation<Blog>
        Blog blog = Blog.builder()
            .name("A neat blog")
            .owner(BlogOwner.builder()
                .name("Joe Swanson")
                .build())
            .build();
        ModelSchema schema = ModelSchema.fromModelClass(Blog.class);
        PendingMutation<Blog> originalMutation = PendingMutation.creation(blog, schema);
        String expectedMutationId = originalMutation.getMutationId().toString();

        // Instantiate the object under test
        PendingMutation.Converter converter = new GsonPendingMutationConverter();

        // Try to construct a record from the PendingMutation instance.
        PendingMutation.PersistentRecord record = converter.toRecord(originalMutation);
        assertNotNull(record);
        assertEquals(expectedMutationId, record.getId());

        // Now, try to convert it back...
        PendingMutation<Blog> reconstructedItemChange = converter.fromRecord(record);
        assertEquals(originalMutation, reconstructedItemChange);
    }
}
