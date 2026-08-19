/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLRequest;

/**
 * Implemented by a {@link GraphQLRequest} that declares which {@link AuthorizationType} should be
 * used to authorize it, overriding the default authorization type of the API it is sent to.
 *
 * <p>The API plugin reads this instead of testing for a concrete request class, so any request type
 * can opt in to per-request authorization. Note that a request which carries an authorization type
 * is deliberately excluded from automatic multi-auth resolution: selecting an authorization type
 * from {@code @auth} rules requires a model schema, and an explicit override is unambiguous.</p>
 */
public interface AuthorizedGraphQLRequest {
    /**
     * Returns the {@link AuthorizationType} to use for this request, or null to use the default
     * authorization type configured for the API.
     * @return the {@link AuthorizationType} for this request, or null to use the API default.
     */
    @Nullable
    AuthorizationType getAuthorizationType();
}
