/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model.query.predicate;

/**
 * A collection of factory methods to build some common {@link QueryPredicate}s.
 */
@SuppressWarnings("checkstyle:WhitespaceAround") // Intentionally using lots of {}
public final class QueryPredicates {
    private QueryPredicates() {}

    /**
     * Builds a {@link QueryPredicate} which applies no predicate; all
     * inputs to a query will therefor be matched.
     * @return An "all match" implementation of a {@link QueryPredicate}.
     */
    public static QueryPredicate matchAll() {
        return new QueryPredicate() {};
    }
}
