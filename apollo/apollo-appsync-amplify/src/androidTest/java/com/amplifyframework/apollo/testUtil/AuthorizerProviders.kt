/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.testUtil

import com.amplifyframework.apollo.appsync.ApolloAmplifyConnector
import com.amplifyframework.apollo.appsync.AppSyncAuthorizer
import com.amplifyframework.apollo.appsync.authorizers.ApiKeyAuthorizer
import com.amplifyframework.apollo.appsync.authorizers.AuthTokenAuthorizer
import com.amplifyframework.apollo.appsync.authorizers.IamAuthorizer

sealed interface AuthorizerProvider {
    val errorMessage: String
    val valid: AppSyncAuthorizer
    val invalid: AppSyncAuthorizer
    val throwing: AppSyncAuthorizer

    suspend fun setupPrerequisites()
    suspend fun teardownPrerequisites()
}

class ApiKeyProvider(connector: ApolloAmplifyConnector) : AuthorizerProvider {
    override val errorMessage = "no api key"
    override val valid = connector.apiKeyAuthorizer()
    override val invalid = ApiKeyAuthorizer("invalid")
    override val throwing = ApiKeyAuthorizer { error(errorMessage) }
    override suspend fun setupPrerequisites() = Unit // no-op
    override suspend fun teardownPrerequisites() = Unit // no-op

    override fun toString() = "ApiKey"
}

class AuthTokenProvider(connector: ApolloAmplifyConnector) : AuthorizerProvider {
    private val testUser = TestUser()

    override val errorMessage = "no auth token"
    override val valid = connector.cognitoUserPoolAuthorizer()
    override val invalid = AuthTokenAuthorizer { "invalid" }
    override val throwing = AuthTokenAuthorizer { error(errorMessage) }

    override suspend fun setupPrerequisites() {
        testUser.signOut() // In case prior test did not cleanup
        testUser.create() // Ensure user already exists
        testUser.signIn()
    }

    override suspend fun teardownPrerequisites() {
        testUser.signOut()
    }

    override fun toString() = "AuthToken"
}

class IamProvider(connector: ApolloAmplifyConnector) : AuthorizerProvider {
    private val testUser = TestUser()

    override val errorMessage = "signature generation failed"
    override val valid = connector.iamAuthorizer()
    override val invalid = IamAuthorizer { emptyMap() }
    override val throwing = IamAuthorizer { error(errorMessage) }

    override suspend fun setupPrerequisites() {
        testUser.signOut() // In case prior test did not cleanup
        testUser.create() // Ensure user already exists
        testUser.signIn()
    }

    override suspend fun teardownPrerequisites() {
        testUser.signOut()
    }

    override fun toString() = "Iam"
}
