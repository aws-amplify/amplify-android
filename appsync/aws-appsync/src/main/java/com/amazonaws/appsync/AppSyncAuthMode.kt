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
package com.amazonaws.appsync

/**
 * The authorization modes supported by AWS AppSync.
 */
enum class AppSyncAuthMode {
    /** API Key authorization. */
    API_KEY,

    /** Amazon Cognito User Pools authorization. */
    USER_POOLS,

    /** OpenID Connect authorization. */
    OIDC,

    /** AWS IAM authorization. */
    IAM,

    /** AWS Lambda custom authorization. */
    LAMBDA
}
