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

package com.amplifyframework.datastore.storage;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ItemChangeMapper}.
 */
public final class ItemChangeMapperTest {
    private String changeId;
    private BlogOwner joe;

    /**
     * Set up some arranged data that is shared across @Tests.
     */
    @Before
    public void setup() {
        changeId = UUID.randomUUID().toString();
        joe = BlogOwner.builder()
            .name("Joe")
            .build();
    }

    /**
     * Try to map a {@link StorageItemChange} that is the result of a customer
     * calling {@link DataStoreCategoryBehavior#save(Model, QueryPredicate, Consumer, Consumer)}
     * to update an existing model.
     * @throws DataStoreException
     *         Not expected for the arranged data. Would only happen if object under test is faulty.
     */
    @Test
    public void mapCustomerUpdate() throws DataStoreException {
        assertEquals(
            DataStoreItemChange.<BlogOwner>builder()
                .initiator(DataStoreItemChange.Initiator.LOCAL)
                .item(joe)
                .itemClass(BlogOwner.class)
                .uuid(changeId)
                .type(DataStoreItemChange.Type.UPDATE)
                .build(),
            ItemChangeMapper.map(StorageItemChange.<BlogOwner>builder()
                .changeId(changeId)
                .initiator(StorageItemChange.Initiator.DATA_STORE_API)
                .item(joe)
                .itemClass(BlogOwner.class)
                .predicate(QueryPredicates.all())
                .type(StorageItemChange.Type.UPDATE)
                .build())
        );
    }

    /**
     * Try to map a {@link StorageItemChange} that is the result of the cloud
     * deleting a model through a subscription event.
     * @throws DataStoreException
     *         Not expected for the arranged data. Would only happen if object under test is faulty.
     */
    @Test
    public void mapCloudDeletion() throws DataStoreException {
        assertEquals(
            DataStoreItemChange.<BlogOwner>builder()
                .initiator(DataStoreItemChange.Initiator.REMOTE)
                .item(joe)
                .itemClass(BlogOwner.class)
                .uuid(changeId)
                .type(DataStoreItemChange.Type.DELETE)
                .build(),
            ItemChangeMapper.map(StorageItemChange.<BlogOwner>builder()
                .changeId(changeId)
                .initiator(StorageItemChange.Initiator.SYNC_ENGINE)
                .item(joe)
                .itemClass(BlogOwner.class)
                .predicate(QueryPredicates.all())
                .type(StorageItemChange.Type.DELETE)
                .build())
        );
    }
}
