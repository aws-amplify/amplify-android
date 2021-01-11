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

package com.amplifyframework.core.model.query.predicate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link QueryPredicate} that matches none of the objects passed to it.
 */
public final class MatchNoneQueryPredicate implements QueryPredicate {
    private MatchNoneQueryPredicate() {}

    /**
     * Creates a new instance of a {@link MatchNoneQueryPredicate}.
     * @return A match-none query predicate
     */
    @NonNull
    public static MatchNoneQueryPredicate instance() {
        return new MatchNoneQueryPredicate();
    }

    @Override
    public boolean evaluate(@Nullable Object target) {
        return false;
    }

    @Override
    public int hashCode() {
        return MatchNoneQueryPredicate.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof MatchNoneQueryPredicate;
    }

    @NonNull
    @Override
    public String toString() {
        return MatchNoneQueryPredicate.class.getSimpleName();
    }

    @Override
    public QueryPredicate and(QueryPredicate predicate) {
        return this;
    }

    @Override
    public QueryPredicate or(QueryPredicate predicate) {
        return predicate;
    }
}
