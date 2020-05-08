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

import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the functionality of the {@link GsonPendingMutationConverter}.
 */
public class GsonPendingMutationConverterTest {
    /**
     * Validate that the {@link GsonPendingMutationConverter} can be
     * used to convert a sample {@link PendingMutation} to a
     * {@link PendingMutation.PersistentRecord}, and vice-versa.
     * @throws DataStoreException from DataStore conversion
     */
    @Test
    public void convertStorageItemChangeToRecordAndBack() throws DataStoreException {
        // Arrange a PendingMutation<Blog>
        String expectedChangeId = UUID.randomUUID().toString();
        Blog blog = Blog.builder()
            .name("A neat blog")
            .owner(BlogOwner.builder()
                .name("Joe Swanson")
                .build())
            .id(expectedChangeId)
            .build();
        PendingMutation<Blog> originalItemChange = PendingMutation.creation(blog, Blog.class);

        // Instantiate the object under test
        PendingMutation.Converter converter = new GsonPendingMutationConverter();

        // Try to construct a record from the StorageItemChange instance.
        PendingMutation.PersistentRecord record = converter.toRecord(originalItemChange);
        assertNotNull(record);
        assertEquals(expectedChangeId, record.getId());

        // Now, try to convert it back...
        PendingMutation<Blog> reconstructedItemChange = converter.fromRecord(record);
        assertEquals(originalItemChange, reconstructedItemChange);
    }
}
