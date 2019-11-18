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

import com.amplifyframework.core.model.Model;

/**
 * A utility class containing static factory methods that generate
 * some useful {@link FilteringPredicates}s.
 */
public final class FilteringPredicates {
    @SuppressWarnings("checkstyle:all") private FilteringPredicates() {}

    /**
     * Builds a predicate which checks if an object has a String-typed
     * field that begins with a provided prefix. For example, if
     * `class Person { String name; }`, and `Person tom = new Person("Tom Smith");`,
     * then beginsWith("name", "Tom").matches(tom) should return true.
     * @param fieldName The name of the field on the subject that should be evaluated
     * @param prefix A String which the fieldName's value might have as a prefix
     * @param <S> The type of the object subject who owns the {@see fieldName}.
     * @return A non-null FilteringPredicate which evaluates a subject by considering
     *         whether or not it has a field, identified by {@see fieldName},
     *         whose value begins with the provided {@see prefix}.
     */
    @NonNull
    static <S extends Model> FilteringPredicate<S> beginsWith(String fieldName, String prefix) {
        return /* TODO! */ (S subject) -> false;
    }

    /**
     * Builds a predicate which checks if a subject's field has a provided value.
     * @param fieldName The name of a field in a subject
     * @param candidateValue A value to compare against subject's fieldName.
     * @param <S> The class type of the subject
     * @param <C> The class type of the candidate value
     * @return A non-null FilteringPredicate which evaluates a subject by
     *         checking if the value of its {@see filedName} is equal to the
     *         provided {@see candidateValue}.
     */
    @NonNull
    static <S extends Model, C> FilteringPredicate<S> equals(String fieldName, C candidateValue) {
        return /* TODO! */ (S subject) -> false;
    }

    /**
     * Builds a predicate which checks if a subject's field is not equal
     * to a provided value.
     * @param fieldName The name of a field in the subject
     * @param candidateValue A value which may or may not match against the subject's field
     * @param <S> The class type of the subject of the predicate
     * @param <C> The class type of the candidate value
     * @return A non-null FilteringPredicate that filters subjects by checking
     *         if the value of subject's {@see fieldName} is not equal to the
     *         provided {@see value}.
     */
    @NonNull
    static <S extends Model, C> FilteringPredicate<S> notEquals(String fieldName, C candidateValue) {
        return /* TODO! */ (S subject) -> false;
    }
}
