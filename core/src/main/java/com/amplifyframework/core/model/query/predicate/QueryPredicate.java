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

import java.util.Collections;

/**
 * Parent class for groups of conditions and individual conditions.
 * This way, through polymorphism, an individual condition can be
 * chained with another individual condition or an existing group
 * of conditions.
 */
public interface QueryPredicate extends Evaluable<Object> {

    /**
     * Return a predicate that evaluates AND connection between this and given predicate.
     * @param predicate the predicate to connect to
     * @return a combined predicate
     */
    QueryPredicate and(QueryPredicate predicate);

    /**
     * Return a predicate that evaluates OR connection between this and given predicate.
     * @param predicate the predicate to connect to
     * @return a combined predicate
     */
    QueryPredicate or(QueryPredicate predicate);

    /**
     * Return a negation of the given predicate.
     * @param predicate the predicate to negate
     * @return a negation of the given predicate
     */
    static QueryPredicate not(QueryPredicate predicate) {
        return new QueryPredicateGroup(QueryPredicateGroup.Type.NOT, Collections.singletonList(predicate));
    }
}
