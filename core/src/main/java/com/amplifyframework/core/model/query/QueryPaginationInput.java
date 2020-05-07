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

package com.amplifyframework.core.model.query;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A simple data structure that holds pagination information that can be applied queries.
 */
public class QueryPaginationInput {

    /**
     * The default page size.
     */
    public static final Integer DEFAULT_LIMIT = 100;

    private final Integer page;
    private final Integer limit;

    private QueryPaginationInput(@NonNull Integer page, @NonNull Integer limit) {
        this.page = page;
        this.limit = limit;
    }

    /**
     * Creates a {@link QueryPaginationInput} in an expressive way, enabling a short
     * and developer friendly to create a new instance.
     *
     * @param page the page number (starting at 0)
     * @return a new instance of <code>QueryPaginationInput</code>.
     */
    public static QueryPaginationInput page(@NonNull Integer page) {
        return new QueryPaginationInput(Objects.requireNonNull(page), DEFAULT_LIMIT);
    }

    /**
     * Utility that creates a <code>QueryPaginationInput</code>
     * with <code>page</code> 0 and <code>limit</code> {@link #DEFAULT_LIMIT}.
     *
     * @return an instance with <code>page</code> 0 and <code>limit</code> {@link #DEFAULT_LIMIT}.
     */
    public static QueryPaginationInput firstPage() {
        return page(0);
    }

    /**
     * Utility that creates a <code>QueryPaginationInput</code>
     * with <code>page</code> 0 and <code>limit</code> 1.
     *
     * @return an instance with <code>page</code> 0 and <code>limit</code> 1.
     */
    public static QueryPaginationInput firstResult() {
        return page(0).withLimit(1);
    }

    /**
     * Sets the limit (i.e. page size) of the current pagination input.
     *
     * @param limit the page size
     * @return a copy of the current {@link QueryPaginationInput} with a new <code>limit</code>.
     */
    public QueryPaginationInput withLimit(@NonNull Integer limit) {
        return new QueryPaginationInput(this.page, limit);
    }

    /**
     * Returns the {@code page} property.
     * @return the {@code page} property.
     */
    public Integer getPage() {
        return page;
    }

    /**
     * Returns the {@code limit} property.
     * @return the {@code limit} property.
     */
    public Integer getLimit() {
        return limit;
    }
}
