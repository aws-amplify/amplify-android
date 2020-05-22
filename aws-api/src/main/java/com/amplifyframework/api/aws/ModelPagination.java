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

package com.amplifyframework.api.aws;

import androidx.annotation.Nullable;

/**
 * TODO.
 */
public final class ModelPagination {

    private static final int DEFAULT_LIMIT = 1000;

    private int limit;
    private final String nextToken;

    private ModelPagination(int limit, String nextToken) {
        this.limit = limit;
        this.nextToken = nextToken;
    }

    /**
     * TODO.
     * @return todo.
     */
    public static ModelPagination firstPage() {
        return nextToken(null);
    }

    /**
     * TODO.
     * @param nextToken todo.
     * @return todo.
     */
    public static ModelPagination nextToken(@Nullable String nextToken) {
        return new ModelPagination(DEFAULT_LIMIT, null);
    }

    /**
     * TODO.
     * @param limit todo.
     * @return todo.
     */
    public ModelPagination withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * TODO.
     * @return todo.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * TODO.
     * @return todo.
     */
    public String getNextToken() {
        return nextToken;
    }
}
