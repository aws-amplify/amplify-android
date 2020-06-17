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

    @Test
    public void testContainsForStringField() throws DataStoreException {
        QueryPredicate predicate = Where.matches(Blog.NAME.contains("something")).getQueryPredicate();
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        assertEquals(1, sqlPredicate.getBindings().size());
        assertEquals("%something%", sqlPredicate.getBindings().get(0));
        assertEquals("name LIKE ?", sqlPredicate.toString());
    }

    @Test
    public void testContainsForNonStringField() throws DataStoreException {
        QueryPredicateOperation<String> predicate = Blog.TAGS.contains("something");
        SQLPredicate sqlPredicate = new SQLPredicate(predicate);
        assertEquals(1, sqlPredicate.getBindings().size());
        assertEquals("something", sqlPredicate.getBindings().get(0));
        assertEquals("? IN tags", sqlPredicate.toString());
    }
}
