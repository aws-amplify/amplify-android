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

/**
 * A simple data structure that holds pagination information that can be applied queries.
 */
public final class QueryPaginationInput {

    /**
     * The default page size.
     */
    public static final int DEFAULT_LIMIT = 100;

    private final int page;
    private final int limit;

    QueryPaginationInput(int page, int limit) {
        this.page = page;
        this.limit = limit;
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
    public int getPage() {
        return page;
    }

    /**
     * Returns the {@code limit} property.
     * @return the {@code limit} property.
     */
    public int getLimit() {
        return limit;
    }
}
