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

package com.amplifyframework.api.aws.sigv4;

import com.amplifyframework.api.ApiException;

/**
 * Interface to provide authentication token
 * from Cognito User Pools to the caller.
 */
public interface CognitoUserPoolsAuthProvider {
    /**
     * Vends the latest valid authentication token
     * from Cognito User Pool session.
     * @return the latest auth token
     * @throws ApiException if retrieving token fails
     */
    String getLatestAuthToken() throws ApiException;

    /**
     * Returns the currently logged in user's name from local cache.
     * @return the currently logged in user's name or null if not available
     * @throws ApiException in case the operation was interrupted by a different thread
     * or there is a problem retrieving the username
     */
    String getUsername() throws ApiException;
}
