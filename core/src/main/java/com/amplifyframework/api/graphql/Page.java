package com.amplifyframework.api.graphql;

public abstract class Page<M> {
    public abstract Iterable<M> getItems();

    public abstract boolean hasNextPage();

    public abstract GraphQLRequest<Page<M>> getRequestForNextPage();
}