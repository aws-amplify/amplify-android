/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.aws.appsync.core

/**
 * Interface for classes that provide different types of authorization for AppSync. AppSync supports various auth
 * modes, including API Key, Cognito User Pools, OIDC, Lambda-based authorization, and IAM policies. Implementations
 * of this interface can be used to provide the specific headers and payloads needed for the auth mode being used.
 */
interface AppSyncAuthorizer {
    suspend fun getAuthorizationHeaders(request: AppSyncRequest): Map<String, String>
}