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

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Implementation of a GraphQL Request serializer for the variables map using Gson.
 */
public final class GsonVariablesSerializer implements GraphQLRequest.VariablesSerializer {
    private final Gson gson;

    /**
     * Creates a new instance of GsonVariablesSerializer, using a
     * default Gson instance.
     */
    public GsonVariablesSerializer() {
        this(GsonFactory.instance());
    }

    /**
     * Creates a new instance of GsonVariablesSerializer, using a custom
     * Gson instance.
     * @param gson Gson instance to use for serialization
     */
    public GsonVariablesSerializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String serialize(Map<String, Object> variables) {
        return gson.toJson(variables);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        return thatObject != null && getClass() == thatObject.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
