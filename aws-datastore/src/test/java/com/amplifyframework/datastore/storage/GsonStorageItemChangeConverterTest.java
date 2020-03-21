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

import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the functionality of the {@link GsonStorageItemChangeConverter}.
 */
public class GsonStorageItemChangeConverterTest {

    /**
     * Validate that the {@link GsonStorageItemChangeConverter} can be
     * used to convert a sample {@link StorageItemChange} to a
     * {@link StorageItemChange.Record}, and vice-versa.
     * @throws DataStoreException from DataStore conversion
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void convertStorageItemChangeToRecordAndBack() throws DataStoreException {
        // Arrange a StorageItemChange<Blog> with an expected change ID
        String expectedChangeId = UUID.randomUUID().toString();
        StorageItemChange<Blog> originalItemChange = StorageItemChange.<Blog>builder()
            .changeId(expectedChangeId)
            .itemClass(Blog.class)
            .item(Blog.builder()
                .name("A neat blog")
                .owner(BlogOwner.builder()
                    .name("Joe Swanson")
                    .build())
                .build())
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .type(StorageItemChange.Type.CREATE)
            .build();

        // Instantiate the object under test
        GsonStorageItemChangeConverter converter = new GsonStorageItemChangeConverter();

        // Try to construct a record from the StorageItemChange instance.
        StorageItemChange.Record record = converter.toRecord(originalItemChange);
        assertNotNull(record);
        assertEquals(expectedChangeId, record.getId());

        // Now, try to convert it back...
        StorageItemChange<Blog> reconstructedItemChange = converter.fromRecord(record);
        assertEquals(expectedChangeId, reconstructedItemChange.changeId().toString());
        assertEquals(originalItemChange, reconstructedItemChange);
    }
}
