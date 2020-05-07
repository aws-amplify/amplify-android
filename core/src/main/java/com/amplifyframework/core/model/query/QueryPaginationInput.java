package com.amplifyframework.core.model.query;

import java.util.Objects;

import androidx.annotation.NonNull;

public class QueryPaginationInput {

    public static final Integer DEFAULT_LIMIT = 100;

    private final Integer page;
    private final Integer limit;

    private QueryPaginationInput(@NonNull Integer page, @NonNull Integer limit) {
        this.page = page;
        this.limit = limit;
    }

    public static QueryPaginationInput page(@NonNull Integer page) {
        return new QueryPaginationInput(Objects.requireNonNull(page), DEFAULT_LIMIT);
    }

    public static QueryPaginationInput firstPage() {
        return page(0);
    }

    public static QueryPaginationInput firstResult() {
        return page(0).withLimit(1);
    }

    public QueryPaginationInput withLimit(@NonNull Integer limit) {
        return new QueryPaginationInput(this.page, limit);
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }
}
