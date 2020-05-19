package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.Page;

final class AppSyncPage<T> extends Page<T> {
    private final GraphQLRequest<Page<T>> requestForNextPage;
    private final Iterable<T> items;

    public AppSyncPage(@NonNull Iterable<T> items,
                       @NonNull GraphQLRequest<Page<T>> request) {
        this.requestForNextPage = request;
        this.items = items;
    }

    @Override
    public Iterable<T> getItems() {
        return items;
    }

    @Override
    public boolean hasNextPage() {
        return requestForNextPage != null;
    }

    @Override
    public GraphQLRequest<Page<T>> getRequestForNextPage() {
        return requestForNextPage;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AppSyncPage page = (AppSyncPage) thatObject;

        return ObjectsCompat.equals(items, page.items) &&
                ObjectsCompat.equals(requestForNextPage, page.requestForNextPage);
    }

    @Override
    public int hashCode() {
        int result = items.hashCode();
        result = 31 * result + (requestForNextPage != null ? requestForNextPage.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphQLResponse.Error{" +
                "items=\'" + items + "\'" +
                ", requestForNextPage=\'" + requestForNextPage + "\'" +
                '}';
    }
}