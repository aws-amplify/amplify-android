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

package com.amplifyframework.api.aws;

/**
 * The type of API for a given endpoint.
 */
public enum EndpointType {

    /**
     * GraphQL backend API.
     */
    GRAPHQL("GraphQL"),

    /**
     * RESTful backend API.
     */
    REST("REST");

    private final String name;

    EndpointType(String name) {
        this.name = name;
    }

    /**
     * Look up an EndpointType by its String name.
     * @param name String representation of an endpoint type
     * @return The corresponding endpoint type
     */
    static EndpointType from(String name) {
        for (final EndpointType endpointType : values()) {
            if (endpointType.name.equals(name)) {
                return endpointType;
            }
        }

        throw new IllegalArgumentException("No such endpoint type: " + name);
    }
}

