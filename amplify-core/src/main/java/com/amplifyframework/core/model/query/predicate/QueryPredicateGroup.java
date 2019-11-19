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

import java.util.ArrayList;
import java.util.List;

/**
 * Groups two conditions (or groups of conditions) by a combination
 * operation or wraps a given condition/group in a negation.
 */
public class QueryPredicateGroup {
    private Type type;
    private List<QueryPredicate> predicates;

    /**
     * Creates a new group given a type to apply to the elements of this group.
     * @param type the type to apply to the elements of this group
     */
    public QueryPredicateGroup(Type type) {
        this.type = type;
        predicates = new ArrayList<>();
    }

    /**
     * Creates a new group given a type to apply to the elements of this group + the elements of this group.
     * @param type the type to apply to the elements of this group
     * @param predicates the operations and/or groups of operations to group together here
     */
    public QueryPredicateGroup(Type type, List<QueryPredicate> predicates) {
        this.type = type;
        this.predicates = predicates;
    }

    /**
     * Return a group connecting this group with another group/operation with an AND type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this group with another group/operation with an AND type
     */
    public QueryPredicateGroup and(QueryPredicate predicate) {
        return null;
    }

    /**
     * Return a group connecting this group with another group/operation with an OR type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this group with another group/operation with an OR type
     */
    public QueryPredicateGroup or(QueryPredicate predicate) {
        return null;
    }

    /**
     * Return a group negating the given group of operations.
     * @param predicate the group to negate
     * @return a group negating the given group of operations
     */
    public static QueryPredicateGroup not(QueryPredicateGroup predicate) {
        return null;
    }

    /**
     * The available types of groupings.
     */
    public enum Type {
        /**
         * Check if one condition AND another condition are true.
         */
        AND,
        /**
         * Check if one condition OR another condition are true.
         */
        OR,
        /**
         * Check if the given condition is NOT true.
         */
        NOT
    }
}
