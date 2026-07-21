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
package com.amplifyframework.connect

import com.amplifyframework.foundation.credentials.AwsCredentials

/**
 * Resolves AWS credentials for signing requests to the Customer Profiles
 * endpoint.
 *
 * All routes use SigV4 (`execute-api`) — both authenticated users (Cognito
 * Identity Pool credentials derived from the user-pool token) and
 * unauthenticated guests (guest Identity Pool credentials). The backend
 * Lambda derives the principal identity from the signer.
 *
 * The Amplify Auth integration layer implements this against
 * `Amplify.Auth.fetchAuthSession()` to extract the Identity Pool AWS
 * credentials.
 */
fun interface ConnectCredentialsProvider {
    /**
     * Resolves the AWS credentials for signing the request.
     *
     * @throws ConnectNotSignedInException if credentials cannot be obtained
     */
    suspend fun resolve(): AwsCredentials
}
