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

package com.amplifyframework.api.graphql.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class that provides methods configure a paginated model-based request.
 */
public final class ModelPagination {

    /**
     * Default page size.
     */
    private static final int DEFAULT_LIMIT = 1000;

    private int limit;
    private final String nextToken;

    /**
     * This class should be created using the factory methods {@link #firstPage()} and {@link #nextToken(String)}.
     * @param limit the page size.
     * @param nextToken the next page token.
     */
    private ModelPagination(int limit, String nextToken) {
        this.limit = limit;
        this.nextToken = nextToken;
    }

    /**
     * Creates a reference to the first page. Same as {@link #nextToken(String)} passing {@code null}.
     * @return an instance of {@link ModelPagination}.
     */
    @NonNull
    public static ModelPagination firstPage() {
        return nextToken(null);
    }

    /**
     * Creates a {@link ModelPagination} with the given {@code nextToken}.
     * @param nextToken the value of the nextToken.
     * @return an instance of {@link ModelPagination}.
     */
    @NonNull
    public static ModelPagination nextToken(@Nullable String nextToken) {
        return new ModelPagination(DEFAULT_LIMIT, nextToken);
    }

    /**
     * Sets the page size/limit.
     * @param limit the size for each page.
     * @return the current instance with the new {@code limit} set.
     */
    @NonNull
    public ModelPagination withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Returns the {@code limit} property.
     * @return the {@code limit} property.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns the {@code nextToken} property.
     * @return the {@code nextToken} property.
     */
    @Nullable
    public String getNextToken() {
        return nextToken;
    }
}
