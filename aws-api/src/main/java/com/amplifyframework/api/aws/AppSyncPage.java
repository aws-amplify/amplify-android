package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.Page;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;

public class AppSyncPage<T> extends Page<T> {
    private final String NEXT_TOKEN_KEY = "nextToken";

    private GraphQLRequest<T> request;
    private String nextToken;

    public AppSyncPage(@NonNull GraphQLResponse<Iterable<T>> response) {
        this(response, null, null);
    }

    public AppSyncPage(@NonNull GraphQLResponse<Iterable<T>> response,
                       @NonNull GraphQLRequest<T> request,
                       String nextToken) {
        super(response);
        this.request = request;
        this.nextToken = nextToken;
    }

    @Override
    public boolean hasNextPage() {
        return nextToken != null;
    }

    @Override
    public GraphQLOperation<T> queryNextPage(@NonNull Consumer<Page<T>> onResponse,
                                             @NonNull Consumer<ApiException> onFailure) {
        this.request.addVariable(NEXT_TOKEN_KEY, nextToken);
        return Amplify.API.pagedQuery(request, onResponse, onFailure);
    }
}
