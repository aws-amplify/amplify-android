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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.annotations.InternalAmplifyApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Groups two conditions (or groups of conditions) by a combination
 * operation or wraps a given condition/group in a negation.
 */
public final class QueryPredicateGroup implements QueryPredicate {
    private final Type type;
    private final List<QueryPredicate> predicates;

    /**
     * Creates a new group given a type to apply to the elements of this group + the elements of this group.
     * @param type the type to apply to the elements of this group
     * @param predicates the operations and/or groups of operations to group together here
     * @throws IllegalArgumentException when the group does not contain any predicate element
     */
    @InternalAmplifyApi
    public QueryPredicateGroup(@NonNull Type type,
                        @NonNull List<QueryPredicate> predicates) {
        this.type = type;
        this.predicates = new ArrayList<>(predicates);
        if (predicates.isEmpty()) {
            throw new IllegalArgumentException("A predicate group " +
                    "must contain at least one predicate element");
        }
    }

    /**
     * Returns this group's operation type (e.g. AND, OR, NOT).
     * @return this group's type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the predicates included in this group.
     * @return the predicates included in this group
     */
    public List<QueryPredicate> predicates() {
        return predicates;
    }

    /**
     * Returns a group with an AND type, containing only the provided predicate.
     * @param predicate the query predicate operation to wrap
     * @return a group with an AND type, containing only the provided predicate.
     */
    public static QueryPredicate andOf(QueryPredicate predicate) {
        return new QueryPredicateGroup(Type.AND, Arrays.asList(predicate));
    }

    /**
     * Return a group connecting this group with another group/operation with an AND type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this group with another group/operation with an AND type
     */
    public QueryPredicateGroup and(QueryPredicate predicate) {
        if (Type.AND.equals(type)) {
            predicates.add(predicate);
            return this;
        }

        return new QueryPredicateGroup(Type.AND, Arrays.asList(this, predicate));
    }

    /**
     * Return a group connecting this group with another group/operation with an OR type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this group with another group/operation with an OR type
     */
    public QueryPredicateGroup or(QueryPredicate predicate) {
        if (Type.OR.equals(type)) {
            predicates.add(predicate);
            return this;
        }

        return new QueryPredicateGroup(Type.OR, Arrays.asList(this, predicate));
    }

    /**
     * Return a group negating the given group of operations.
     * @param predicate the group to negate
     * @return a group negating the given group of operations
     */
    public static QueryPredicateGroup not(QueryPredicateGroup predicate) {
        return new QueryPredicateGroup(Type.NOT, Collections.singletonList(predicate));
    }

    /**
     * Evaluate the combination of operations associated with
     * this group of predicates.
     * @param object The object to evaluate against
     * @return Evaluated result of this logical combination
     * @throws IllegalArgumentException when the object contains
     *          a field with data type that cannot be evaluated
     * @throws IllegalStateException when the predicate group does
     *          not contain any predicate element
     */
    @Override
    public boolean evaluate(Object object) throws IllegalArgumentException {
        switch (type) {
            case OR:
                for (QueryPredicate predicate : predicates) {
                    if (predicate.evaluate(object)) {
                        return true;
                    }
                }
                return false;
            case AND:
                for (QueryPredicate predicate : predicates) {
                    if (!predicate.evaluate(object)) {
                        return false;
                    }
                }
                return true;
            case NOT:
                // predicates should never be empty!
                QueryPredicate predicate = predicates.get(0);
                return !predicate.evaluate(object);
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            QueryPredicateGroup group = (QueryPredicateGroup) obj;

            return ObjectsCompat.equals(type(), group.type()) &&
                    ObjectsCompat.equals(predicates(), group.predicates());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                type(),
                predicates()
        );
    }

    @Override
    public String toString() {
        return "QueryPredicateGroup { " +
            "type: " + type() +
            ", predicates: " + predicates() +
            " }";
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
