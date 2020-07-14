/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.testmodels.todo.Todo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AppSyncGraphQlRequestTest {
    /**
     * Verify that newBuilder().build() returns an AppSyncGraphQLRequest equal to the original.
     * @throws AmplifyException on failure to build request.
     */
    @Test
    public void newBuilderBuildsRequestEqualToOriginal() throws AmplifyException {
        AppSyncGraphQLRequest<Todo> original = AppSyncGraphQLRequest.builder()
                .modelClass(Todo.class)
                .operation(QueryType.GET)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .responseType(Todo.class)
                .variable("foo", "String!", "bar")
                .build();

        AppSyncGraphQLRequest<Todo> newInstance = original.newBuilder().build();
        assertEquals(original, newInstance);
        assertEquals(original.getContent(), newInstance.getContent());
        assertEquals(original.toString(), newInstance.toString());
    }
}
