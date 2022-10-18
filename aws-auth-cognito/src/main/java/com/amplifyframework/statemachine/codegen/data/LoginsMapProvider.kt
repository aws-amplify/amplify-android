/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.statemachine.codegen.data

sealed class LoginsMapProvider {
    data class UnAuthLogins(override val logins: Map<String, String> = mapOf()) : LoginsMapProvider()

    data class CognitoUserPoolLogins(
        private val region: String? = "",
        private val poolId: String? = "",
        private val idToken: String
    ) : LoginsMapProvider() {

        /**
         * Amazon Cognito user pool: cognito-idp.<region>.amazonaws.com/<YOUR_USER_POOL_ID>,
         * for example, cognito-idp.us-east-1.amazonaws.com/us-east-1_123456789.
         */
        val providerName = "cognito-idp.$region.amazonaws.com/$poolId"

        override val logins = mapOf(providerName to idToken)
    }

    data class AuthProviderLogins(private val federatedToken: FederatedToken) : LoginsMapProvider() {
        override val logins = mapOf(federatedToken.providerName to federatedToken.token)
    }

    abstract val logins: Map<String, String>
}
