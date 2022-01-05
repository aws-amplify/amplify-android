package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Copy of DefaultGraphQLRequestOptions, with LeafSerializationBehavior set to JUST_ID
 * and maxDepth of 1
 */
public class JustIDGraphQLRequestOptions implements GraphQLRequestOptions {
    private static final String ITEMS_KEY = "items";
    private static final String NEXT_TOKEN_KEY = "nextToken";

    @NonNull
    @Override
    public List<String> paginationFields() {
        return Collections.singletonList(NEXT_TOKEN_KEY);
    }

    @NonNull
    @Override
    public List<String> modelMetaFields() {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public String listField() {
        return ITEMS_KEY;
    }

    @Override
    public int maxDepth() {
        return 1;
    }

    @NonNull
    @Override
    public LeafSerializationBehavior leafSerializationBehavior() {
        return LeafSerializationBehavior.JUST_ID;
    }
}
