package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLPredicate;
import com.amplifyframework.testmodels.ratingsblog.Blog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SQLPredicateTest {

    /**
     * Test contains in the context of a String field.
     * @throws DataStoreException
     */
    @Test
    public void testContainsForStringField() throws DataStoreException {
        QueryPredicate predicate = Where.matches(Blog.NAME.contains("something")).getQueryPredicate();
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        assertStringResults(sqlPredicate, "name");
    }

    /**
     * Test contains in the context of a list.
     * @throws DataStoreException
     */
    @Test
    public void testContainsForStringList() throws DataStoreException {
        QueryPredicateOperation<String> predicate = Blog.TAGS.contains("something");
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        assertStringResults(sqlPredicate, "tags");
    }

    private void assertStringResults(SQLPredicate sqlPredicate, String fieldName) {
        assertEquals(1, sqlPredicate.getBindings().size());
        assertEquals("something", sqlPredicate.getBindings().get(0));
        assertEquals("instr(" + fieldName + ",?)", sqlPredicate.toString());
    }
}
