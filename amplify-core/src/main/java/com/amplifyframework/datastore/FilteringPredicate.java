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

package com.amplifyframework.datastore;

/**
 * A Filtering Predicate defines matching rules on a subject.
 * For example, given:
 * FilteringPredicate&lt;Object&gt; isNumber = subject -> (subject instanceof Number);
 * isNumber.matches("Tony Balogne") // false
 * isNumber.matches(4); // true
 *
 * @param <T> The type of the subject that is being considered by the predicate.
 */
@FunctionalInterface
public interface FilteringPredicate<T> {

    /**
     * Defines matching rules on a subject.
     * @param subject An object which may or may not match defined criteria
     * @return true, if the subject provides a match, false, otherwise
     */
    boolean matches(T subject);
}
