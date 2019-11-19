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
 * A single source for developers to see what types of comparison operators they can create.
 */
public final class QueryOperatorFactory {

    /**
     * Private constructor to prevent developers from instantiating this factory.
     */
    private QueryOperatorFactory() { }

    /**
     * Returns an equality comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the equality condition
     */
    public static EqualQueryOperator equalTo(Object value) {
        return new EqualQueryOperator(value);
    }
}
