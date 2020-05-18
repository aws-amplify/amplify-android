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

/**
 * Query DSL for pagination.
 */
public final class Page {

    /**
     * The default page size.
     */
    public static final int DEFAULT_LIMIT = 100;

    private Page() {}

    /**
     * Creates a {@link QueryPaginationInput} in an expressive way, enabling a short
     * and developer friendly to create a new instance.
     *
     * @param page the page number (starting at 0)
     * @return a new instance of <code>QueryPaginationInput</code>.
     */
    public static QueryPaginationInput startingAt(final int page) {
        return new QueryPaginationInput(page, DEFAULT_LIMIT);
    }

    /**
     * Utility that creates a <code>QueryPaginationInput</code>
     * with <code>page</code> 0 and <code>limit</code> {@link #DEFAULT_LIMIT}.
     *
     * @return an instance with <code>page</code> 0 and <code>limit</code> {@link #DEFAULT_LIMIT}.
     */
    public static QueryPaginationInput firstPage() {
        return startingAt(0);
    }

    /**
     * Utility that creates a <code>QueryPaginationInput</code>
     * with <code>page</code> 0 and <code>limit</code> 1.
     *
     * @return an instance with <code>page</code> 0 and <code>limit</code> 1.
     */
    public static QueryPaginationInput firstResult() {
        return startingAt(0).withLimit(1);
    }
}
