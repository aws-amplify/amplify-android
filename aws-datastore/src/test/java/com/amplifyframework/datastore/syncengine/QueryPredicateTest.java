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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreSyncExpression;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueryPredicateTest {
    private QueryPredicateProvider queryPredicateProvider;
    private QueryPredicate expectedPredicate;
    private DataStoreSyncExpression blogSyncExpression;

    /**
     * Setup the queryPredicateProvider.
     * @throws DataStoreException on failure to build {@link DataStoreConfiguration}
     */
    @Before
    public void setup() throws DataStoreException {
        expectedPredicate = BlogOwner.NAME.beginsWith(RandomString.string());
        blogSyncExpression = () -> expectedPredicate;
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration.builder()
                .syncExpression(BlogOwner.class, blogSyncExpression)
                .build();
        queryPredicateProvider = new QueryPredicateProvider(() -> dataStoreConfiguration);
    }

    /**
     * Verify getPredicate() returns QueryPredicates.all() before the resolvePredicates() is called.
     */
    @Test
    public void getPredicateReturnsAllBeforeResolve() {
        // Before the predicates have been resolved, verify that the provider just returns QueryPredicates.all().
        assertEquals(QueryPredicates.all(), queryPredicateProvider.getPredicate(BlogOwner.class.getSimpleName()));
    }

    /**
     * Verify getPredicate() returns the expected {@link QueryPredicate}.
     * @throws DataStoreException on failure to call resolvePredicates.
     */
    @Test
    public void getPredicateReturnsResolvedPredicate() throws DataStoreException {
        // Act
        queryPredicateProvider.resolvePredicates();

        // Now expect the QueryPredicate to be returned.
        assertEquals(expectedPredicate, queryPredicateProvider.getPredicate(BlogOwner.class.getSimpleName()));
    }
}
