package com.amplifyframework.core.model.query;

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import static com.amplifyframework.core.model.query.QueryPaginationInput.firstResult;

public final class QueryOptions {

    private QueryPredicate queryPredicate;

    private QueryPaginationInput paginationInput;

    private QueryOptions() {}

    public static QueryOptions all() {
        return new QueryOptions();
    }

    public static QueryOptions where(QueryPredicate queryPredicate) {
        final QueryOptions options = new QueryOptions();
        options.queryPredicate = queryPredicate;
        return options;
    }

    public static QueryOptions byId(String id) {
        return where(QueryField.field("id").eq(id)).paginated(firstResult());
    }

    public QueryOptions paginated(QueryPaginationInput paginationInput) {
        this.paginationInput = paginationInput;
        return this;
    }

    public QueryPredicate getQueryPredicate() {
        return queryPredicate;
    }

    public QueryPaginationInput getPaginationInput() {
        return paginationInput;
    }

}
