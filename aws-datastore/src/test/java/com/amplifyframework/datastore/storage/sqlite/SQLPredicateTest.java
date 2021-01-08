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

package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLPredicate;
import com.amplifyframework.testmodels.ratingsblog.Blog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SQLPredicateTest {

    /**
     * Test that MATCH ALL predicate is correctly parsed to an
     * expression that is always true.
     * @throws DataStoreException if parsing fails
     */
    @Test
    public void testMatchAllPredicate() throws DataStoreException {
        QueryPredicate predicate = QueryPredicates.all();
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        assertEquals("1 = 1", sqlPredicate.toString());
        assertTrue(sqlPredicate.getBindings().isEmpty());
    }

    /**
     * Test that MATCH NONE predicate is correctly parsed to an
     * expression that is always false.
     * @throws DataStoreException if parsing fails
     */
    @Test
    public void testMatchNonePredicate() throws DataStoreException {
        QueryPredicate predicate = QueryPredicates.none();
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        assertEquals("1 = 0", sqlPredicate.toString());
        assertTrue(sqlPredicate.getBindings().isEmpty());
    }

    /**
     * Test contains in the context of a String field.
     * @throws DataStoreException Not thrown.
     */
    @Test
    public void testContainsForStringField() throws DataStoreException {
        QueryPredicate predicate = Where.matches(Blog.NAME.contains("something")).getQueryPredicate();
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        validateSQLExpressionForContains(sqlPredicate, "name");
    }

    /**
     * Test contains in the context of a list.
     * @throws DataStoreException Not thrown.
     */
    @Test
    public void testContainsForStringList() throws DataStoreException {
        QueryPredicateOperation<String> predicate = Blog.TAGS.contains("something");
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        validateSQLExpressionForContains(sqlPredicate, "tags");
    }

    private void validateSQLExpressionForContains(SQLPredicate sqlPredicate, String fieldName) {
        assertEquals(1, sqlPredicate.getBindings().size());
        assertEquals("something", sqlPredicate.getBindings().get(0));
        assertEquals("instr(" + fieldName + ",?) > 0", sqlPredicate.toString());
    }
}
