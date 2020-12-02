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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.GraphQLRequestOptions;

import java.util.Arrays;
import java.util.List;

/**
 * Defines options for building a GraphQLRequest when using the DataStore category.
 */
public final class DataStoreGraphQLRequestOptions implements GraphQLRequestOptions {
    private static final String ITEMS_KEY = "items";
    private static final String NEXT_TOKEN_KEY = "nextToken";
    private static final String STARTED_AT_KEY = "startedAt";
    private static final String DELETED_KEY = "_deleted";
    private static final String VERSION_KEY = "_version";
    private static final String LAST_CHANGED_AT_KEY = "_lastChangedAt";

    @NonNull
    @Override
    public List<String> paginationFields() {
        return Arrays.asList(NEXT_TOKEN_KEY, STARTED_AT_KEY);
    }

    @NonNull
    @Override
    public List<String> modelMetaFields() {
        return Arrays.asList(VERSION_KEY, DELETED_KEY, LAST_CHANGED_AT_KEY);
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
}
