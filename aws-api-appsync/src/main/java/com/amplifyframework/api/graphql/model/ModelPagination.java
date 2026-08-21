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

    /**
     * This class should be created using the factory methods {@link #firstPage()} or {@link #limit(int)}.
     * @param limit the page size.
     */
    private ModelPagination(int limit) {
        this.limit = limit;
    }

    /**
     * Creates a reference to the first page.
     * @return an instance of {@link ModelPagination}.
     */
    @NonNull
    public static ModelPagination firstPage() {
        return limit(DEFAULT_LIMIT);
    }

    /**
     * Creates a {@link ModelPagination} with the given {@code limit}.
     * @param limit the page size
     * @return an instance of {@link ModelPagination}.
     */
    @NonNull
    public static ModelPagination limit(@Nullable int limit) {
        return new ModelPagination(limit);
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
}
