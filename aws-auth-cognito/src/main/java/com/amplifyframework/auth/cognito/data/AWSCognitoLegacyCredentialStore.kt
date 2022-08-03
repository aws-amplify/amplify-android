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

package com.amplifyframework.auth.cognito.data

import android.content.Context
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import java.util.Locale

class AWSCognitoLegacyCredentialStore(
    val context: Context,
    private val authConfiguration: AuthConfiguration,
    keyValueRepoFactory: KeyValueRepositoryFactory = KeyValueRepositoryFactory()
) : AuthCredentialStore {

    companion object {
        const val AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER: String = "com.amazonaws.android.auth"
        const val APP_LOCAL_CACHE = "CognitoIdentityProviderCache"

        private const val ID_KEY: String = "identityId"
        private const val AK_KEY: String = "accessKey"
        private const val SK_KEY: String = "secretKey"
        private const val ST_KEY: String = "sessionToken"
        private const val EXP_KEY: String = "expirationDate"

        private const val APP_LAST_AUTH_USER = "LastAuthUser"
        private const val APP_LOCAL_CACHE_KEY_PREFIX = "CognitoIdentityProvider"

        private const val TOKEN_TYPE_ID = "idToken"
        private const val TOKEN_TYPE_ACCESS = "accessToken"
        private const val TOKEN_TYPE_REFRESH = "refreshToken"

        // TODO check if below exists
        private const val TOKEN_EXPIRATION = "tokenExpiration"
    }

    private val idAndCredentialsKeyValue: KeyValueRepository =
        keyValueRepoFactory.create(context, AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER)

    private val tokensKeyValue: KeyValueRepository = keyValueRepoFactory.create(
        context,
        APP_LOCAL_CACHE
    )

    @Synchronized
    override fun saveCredential(credential: AmplifyCredential) {
        // no-op
    }

    @Synchronized
    override fun retrieveCredential(): AmplifyCredential {
        val cognitoUserPoolTokens = retrieveCognitoUserPoolTokens()
        val awsCredentials = retrieveAWSCredentials()
        val identityId = retrieveIdentityId()
        return when {
            awsCredentials != null && identityId != null -> when (cognitoUserPoolTokens) {
                null -> AmplifyCredential.IdentityPool(identityId, awsCredentials)
                else -> AmplifyCredential.UserAndIdentityPool(cognitoUserPoolTokens, identityId, awsCredentials)
            }
            cognitoUserPoolTokens != null -> AmplifyCredential.UserPool(cognitoUserPoolTokens)
            else -> AmplifyCredential.Empty
        }
    }

    override fun deleteCredential() {
        deleteAWSCredentials()
        deleteIdentityId()
        deleteCognitoUserPoolTokens()
    }

    private fun deleteCognitoUserPoolTokens() {
        val keys = getTokenKeys()

        keys[TOKEN_TYPE_ID]?.let { tokensKeyValue.remove(it) }
        keys[TOKEN_TYPE_ACCESS]?.let { tokensKeyValue.remove(it) }
        keys[TOKEN_TYPE_REFRESH]?.let { tokensKeyValue.remove(it) }
        keys[TOKEN_EXPIRATION]?.let { tokensKeyValue.remove(it) }
    }

    private fun deleteIdentityId() {
        idAndCredentialsKeyValue.remove(namespace(ID_KEY))
    }

    private fun deleteAWSCredentials() {
        idAndCredentialsKeyValue.apply {
            remove(namespace(AK_KEY))
            remove(namespace(SK_KEY))
            remove(namespace(ST_KEY))
            remove(namespace(EXP_KEY))
        }
    }

    private fun retrieveAWSCredentials(): AWSCredentials? {
        val accessKey = idAndCredentialsKeyValue.get(namespace(AK_KEY))
        val secretKey = idAndCredentialsKeyValue.get(namespace(SK_KEY))
        val sessionToken = idAndCredentialsKeyValue.get(namespace(ST_KEY))
        val expiration = idAndCredentialsKeyValue.get(namespace(EXP_KEY))?.toLongOrNull()

        return if (accessKey == null && secretKey == null && sessionToken == null) {
            null
        } else AWSCredentials(accessKey, secretKey, sessionToken, expiration)
    }

    private fun retrieveIdentityId(): String? {
        return idAndCredentialsKeyValue.get(namespace(ID_KEY))
    }

    private fun retrieveCognitoUserPoolTokens(): CognitoUserPoolTokens? {
        val keys = getTokenKeys()

        val idToken = keys[TOKEN_TYPE_ID]?.let { tokensKeyValue.get(it) }
        val accessToken = keys[TOKEN_TYPE_ACCESS]?.let { tokensKeyValue.get(it) }
        val refreshToken = keys[TOKEN_TYPE_REFRESH]?.let { tokensKeyValue.get(it) }
        val expiration = keys[TOKEN_EXPIRATION]?.let { tokensKeyValue.get(it) }?.toLongOrNull()

        return if (idToken == null && accessToken == null && refreshToken == null) {
            return null
        } else {
            CognitoUserPoolTokens(idToken, accessToken, refreshToken, expiration)
        }
    }

    private fun getTokenKeys(): Map<String, String> {
        val appClient = authConfiguration.userPool?.appClient

        val userIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            APP_LAST_AUTH_USER
        )

        val userId = tokensKeyValue.get(userIdTokenKey)

        val cachedIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_TYPE_ID
        )
        val cachedAccessTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_TYPE_ACCESS
        )
        val cachedRefreshTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_TYPE_REFRESH
        )

        val cachedTokenExpirationKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_EXPIRATION
        )

        return mapOf(
            TOKEN_TYPE_ID to cachedIdTokenKey,
            TOKEN_TYPE_ACCESS to cachedAccessTokenKey,
            TOKEN_TYPE_REFRESH to cachedRefreshTokenKey,
            TOKEN_EXPIRATION to cachedTokenExpirationKey
        )
    }

    // prefix the key with identity pool id
    private fun namespace(key: String): String = getIdentityPoolId() + "." + key

    private fun getIdentityPoolId(): String? {
        return authConfiguration.identityPool?.poolId
    }
}
