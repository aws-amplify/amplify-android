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
 * A simple data structure that holds sort information that can be applied queries.
 */
public class QuerySortBy {
    private final String field;
    private final QuerySortOrder sortOrder;

    /**
     * Constructor for {@code QuerySortBy}.
     *
     * @param field name of field to sort by.
     * @param sortOrder order to sort by, either ASCENDING or DESCENDING.
     */
    public QuerySortBy(String field, QuerySortOrder sortOrder) {
        this.field = field;
        this.sortOrder = sortOrder;
    }
}
