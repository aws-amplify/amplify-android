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
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import java.util.Locale
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AWSCognitoLegacyCredentialStoreTest {

    companion object {
        private const val IDENTITY_POOL_ID: String = "identityPoolID"
        private const val USER_POOL_ID: String = "userPoolID"

        private const val prefix = "CognitoIdentityProvider"
        private const val appClient = "appClientId"
        private const val userId = "userId"
        private val userIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s",
            prefix,
            appClient,
            "LastAuthUser"
        )
        private val cachedIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            prefix,
            appClient,
            userId,
            "idToken"
        )
        private val cachedAccessTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            prefix,
            appClient,
            userId,
            "accessToken"
        )
        private val cachedRefreshTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            prefix,
            appClient,
            userId,
            "refreshToken"
        )
        private val cachedTokenExpirationKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            prefix,
            appClient,
            userId,
            "tokenExpiration"
        )
    }

    @Mock
    private lateinit var mockConfig: AuthConfiguration

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockKeyValue: KeyValueRepository

    @Mock
    private lateinit var mockFactory: KeyValueRepositoryFactory

    private lateinit var persistentStore: AuthCredentialStore

    @Before
    fun setup() {
        `when`(
            mockFactory.create(
                mockContext,
                AWSCognitoLegacyCredentialStore.AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER,
                true,
            )
        ).thenReturn(mockKeyValue)

        `when`(
            mockFactory.create(
                mockContext,
                AWSCognitoLegacyCredentialStore.APP_LOCAL_CACHE,
                true,
            )
        ).thenReturn(mockKeyValue)
    }

    @Test
    fun testRetrieveCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig, mockFactory)

        val actual = persistentStore.retrieveCredential()

        Assert.assertEquals(actual, getCredential())
    }

    private fun setupKeyValueGetters() {
        // Tokens
        `when`(mockKeyValue.get(userIdTokenKey)).thenReturn("userId")
        `when`(mockKeyValue.get(cachedIdTokenKey)).thenReturn("idToken")
        `when`(mockKeyValue.get(cachedAccessTokenKey)).thenReturn("accessToken")
        `when`(mockKeyValue.get(cachedRefreshTokenKey)).thenReturn("refreshToken")
        `when`(mockKeyValue.get(cachedTokenExpirationKey)).thenReturn("123123")

        // AWS Creds
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"accessKey"}")).thenReturn("accessKeyId")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"secretKey"}")).thenReturn("secretAccessKey")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"sessionToken"}")).thenReturn("sessionToken")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"expirationDate"}")).thenReturn("123123")

        // Identity ID
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"identityId"}")).thenReturn("identityPool")
    }

    private fun setupIdentityPoolConfig() {
        `when`(mockConfig.identityPool).thenReturn(
            IdentityPoolConfiguration {
                this.poolId = IDENTITY_POOL_ID
            }
        )
    }

    private fun setupUserPoolConfig() {
        `when`(mockConfig.userPool).thenReturn(
            UserPoolConfiguration {
                this.poolId = USER_POOL_ID
                this.appClientId = appClient
            }
        )
    }

    private fun getCredential(): AmplifyCredential {
        return AmplifyCredential.UserAndIdentityPool(
            CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", 123123),
            "identityPool",
            AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", 123123)
        )
    }
}
