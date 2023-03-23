/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import java.util.Date
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
        private const val deviceCachePrefix = "CognitoIdentityProviderDeviceCache"
        private const val appClient = "appClientId"
        private const val userId = "username"
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

        private const val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcm5hbW" +
            "UiOiJhbXBsaWZ5X3VzZXIiLCJpYXQiOjE1MTYyMzkwMjJ9.zBiQ0guLRX34pUEYLPyDxQAyDDlXmL0JY7kgPWAHZos"

        private const val userDeviceDetailsCacheKey = "$deviceCachePrefix.$USER_POOL_ID.%s"
        private val deviceDetailsCacheKey = String.format(userDeviceDetailsCacheKey, userId)
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
                AWSCognitoLegacyCredentialStore.APP_TOKENS_INFO_CACHE,
                true,
            )
        ).thenReturn(mockKeyValue)

        `when`(
            mockFactory.create(
                mockContext,
                AWSCognitoLegacyCredentialStore.AWS_MOBILE_CLIENT_PROVIDER,
                true,
            )
        ).thenReturn(mockKeyValue)

        `when`(mockFactory.create(mockContext, deviceDetailsCacheKey, true)).thenReturn(mockKeyValue)
    }

    @Test
    fun testRetrieveSRPCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig, mockFactory)

        val actual = persistentStore.retrieveCredential()

        Assert.assertEquals(actual, getSRPCredential())
    }

    @Test
    fun testRetrieveDeviceMetaData() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig, mockFactory)

        val actual = persistentStore.retrieveDeviceMetadata("username")

        Assert.assertEquals(actual, getDeviceMetaData())
    }

    @Test
    fun testRetrieveHostedUICredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        `when`(mockKeyValue.get("signInMode")).thenReturn("2")
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig, mockFactory)

        val actual = persistentStore.retrieveCredential()

        Assert.assertEquals(actual, getHostedUICredential())
    }

    private fun setupKeyValueGetters() {
        // Tokens
        `when`(mockKeyValue.get(userIdTokenKey)).thenReturn("username")
        `when`(mockKeyValue.get(cachedIdTokenKey)).thenReturn("idToken")
        `when`(mockKeyValue.get(cachedAccessTokenKey)).thenReturn(dummyToken)
        `when`(mockKeyValue.get(cachedRefreshTokenKey)).thenReturn("refreshToken")
        `when`(mockKeyValue.get(cachedTokenExpirationKey)).thenReturn("123123")

        // Device Metadata
        `when`(mockKeyValue.get("DeviceKey")).thenReturn("someDeviceKey")
        `when`(mockKeyValue.get("DeviceGroupKey")).thenReturn("someDeviceGroupKey")
        `when`(mockKeyValue.get("DeviceSecret")).thenReturn("someSecret")

        // AWS Creds
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"accessKey"}")).thenReturn("accessKeyId")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"secretKey"}")).thenReturn("secretAccessKey")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"sessionToken"}")).thenReturn("sessionToken")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"expirationDate"}")).thenReturn("123123")

        // Identity ID
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"identityId"}")).thenReturn("identityPool")

        // Mobile Client SignInMethod
        `when`(mockKeyValue.get("signInMode")).thenReturn(null)
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

    private fun getSRPCredential(): AmplifyCredential {
        return AmplifyCredential.UserAndIdentityPool(
            SignedInData(
                "1234567890",
                "amplify_user",
                Date(0),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                CognitoUserPoolTokens("idToken", dummyToken, "refreshToken", 123123)
            ),
            "identityPool",
            AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", 123123)
        )
    }

    private fun getDeviceMetaData(): DeviceMetadata {
        return AmplifyCredential.DeviceData(
            DeviceMetadata.Metadata(
                "someDeviceKey",
                "someDeviceGroupKey",
                "someSecret"
            )
        ).deviceMetadata
    }

    private fun getHostedUICredential(): AmplifyCredential {
        return AmplifyCredential.UserAndIdentityPool(
            SignedInData(
                "1234567890",
                "amplify_user",
                Date(0),
                SignInMethod.HostedUI(),
                CognitoUserPoolTokens("idToken", dummyToken, "refreshToken", 123123)
            ),
            "identityPool",
            AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", 123123)
        )
    }
}
