package com.amplifyframework.api.graphql;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.core.Consumer;

public abstract class Page<T> {
    private final GraphQLResponse<Iterable<T>> response;

    public abstract boolean hasNextPage();

    public abstract GraphQLOperation<T> queryNextPage(@NonNull Consumer<Page<T>> onResponse,
                                                      @NonNull Consumer<ApiException> onFailure);

    protected Page(GraphQLResponse<Iterable<T>> response) {
        this.response = response;
    }

    public GraphQLResponse<Iterable<T>> getResponse() {
        return response;
    }
}
