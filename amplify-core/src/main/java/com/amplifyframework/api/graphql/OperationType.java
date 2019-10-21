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

package com.amplifyframework.api.graphql;

/**
 * Enum of GraphQL operation types.
 */
public enum OperationType {
    /** GraphQL query. */
    QUERY,

    /** GraphQL mutation. */
    MUTATION,

    /** GraphQL subscription. */
    SUBSCRIPTION;

    /**
     * Gets the string value of operation.
     * @return string of operation type
     */
    public String value() {
        switch (this) {
            case QUERY:
                return "query";
            case MUTATION:
                return "mutation";
            case SUBSCRIPTION:
                return "subscription";
            default:
                return null;
        }
    }
}
