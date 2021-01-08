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

import java.util.Collections;

/**
 * A {@link QueryPredicate} that matches any/all objects passed to it.
 */
public final class MatchAllQueryPredicate implements QueryPredicate {
    private MatchAllQueryPredicate() {}

    /**
     * Creates a new instance of a {@link MatchAllQueryPredicate}.
     * @return A match-all query predicate
     */
    @NonNull
    public static MatchAllQueryPredicate instance() {
        return new MatchAllQueryPredicate();
    }

    @Override
    public boolean evaluate(@Nullable Object target) {
        return true;
    }

    @Override
    public int hashCode() {
        return MatchAllQueryPredicate.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof MatchAllQueryPredicate;
    }

    @NonNull
    @Override
    public String toString() {
        return MatchAllQueryPredicate.class.getSimpleName();
    }

    @Override
    public QueryPredicateGroup and(QueryPredicate predicate) {
        return new QueryPredicateGroup(QueryPredicateGroup.Type.AND, Collections.singletonList(predicate));
    }

    @Override
    public QueryPredicateGroup or(QueryPredicate predicate) {
        return new QueryPredicateGroup(QueryPredicateGroup.Type.OR, Collections.singletonList(this));
    }
}
