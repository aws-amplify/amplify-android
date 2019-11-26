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

import com.amplifyframework.testmodels.MaritalStatus;
import com.amplifyframework.testmodels.Person;

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
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void convertStorageItemChangeToRecordAndBack() {
        // Arrange a StorageItemChange<Person> with an expected change ID
        String expectedChangeId = UUID.randomUUID().toString();
        StorageItemChange<Person> originalItemChange = StorageItemChange.<Person>builder()
            .changeId(expectedChangeId)
            .itemClass(Person.class)
            .item(Person.builder()
                .firstName("Tabitha")
                .lastName("Stevens")
                .age(52)
                .relationship(MaritalStatus.married)
                .build())
            .initiator(StorageItemChange.Initiator.DATA_STORE_API)
            .type(StorageItemChange.Type.SAVE)
            .build();

        // Instantiate the object under test
        GsonStorageItemChangeConverter converter = new GsonStorageItemChangeConverter();

        // Try to construct a record from the StorageItemChange instance.
        StorageItemChange.Record record = converter.toRecord(originalItemChange);
        assertNotNull(record);
        assertEquals(expectedChangeId, record.getId());

        // Now, try to convert it back...
        StorageItemChange<Person> reconstructedItemChange = converter.fromRecord(record);
        assertEquals(expectedChangeId, reconstructedItemChange.changeId().toString());
        assertEquals(originalItemChange, reconstructedItemChange);
    }
}
