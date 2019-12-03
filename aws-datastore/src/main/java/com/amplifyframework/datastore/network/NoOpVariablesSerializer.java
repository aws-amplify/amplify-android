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

package com.amplifyframework.datastore.network;

import com.amplifyframework.api.graphql.GraphQLRequest;

import java.util.Map;

/*
 * A variables serializer which doesn't actually do anything.
 * We don't actually pass variables to the API layer, we just build
 * raw documents with immediate values inlined.
 */
final class NoOpVariablesSerializer implements GraphQLRequest.VariablesSerializer {
    @Override
    public String serialize(Map<String, Object> variables) {
        return null; // Should never get called...
    }
}
