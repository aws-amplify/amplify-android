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
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
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

        private const val PREFIX = "CognitoIdentityProvider"
        private const val DEVICE_CACHE_PREFIX = "CognitoIdentityProviderDeviceCache"
        private const val APP_CLIENT = "appClientId"
        private const val USER_ID = "username"
        private val userIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s",
            PREFIX,
            APP_CLIENT,
            "LastAuthUser"
        )
        private val cachedIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            PREFIX,
            APP_CLIENT,
            USER_ID,
            "idToken"
        )
        private val cachedAccessTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            PREFIX,
            APP_CLIENT,
            USER_ID,
            "accessToken"
        )
        private val cachedRefreshTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            PREFIX,
            APP_CLIENT,
            USER_ID,
            "refreshToken"
        )
        private val cachedTokenExpirationKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            PREFIX,
            APP_CLIENT,
            USER_ID,
            "tokenExpiration"
        )

        private const val DUMMY_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcm5hbW" +
            "UiOiJhbXBsaWZ5X3VzZXIiLCJpYXQiOjE1MTYyMzkwMjJ9.zBiQ0guLRX34pUEYLPyDxQAyDDlXmL0JY7kgPWAHZos"

        private const val USER_DEVICE_DETAILS_CACHE_KEY = "$DEVICE_CACHE_PREFIX.$USER_POOL_ID.%s"
        private val deviceDetailsCacheKey = String.format(USER_DEVICE_DETAILS_CACHE_KEY, USER_ID)

        private const val EXPIRATION_TIMESTAMP_IN_SEC: Long = 1714431706
        private const val EXPIRATION_TIMESTAMP_IN_MS: Long = 1714431706486
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
                AWSCognitoLegacyCredentialStore.AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER
            )
        ).thenReturn(mockKeyValue)

        `when`(
            mockFactory.create(
                mockContext,
                AWSCognitoLegacyCredentialStore.APP_TOKENS_INFO_CACHE
            )
        ).thenReturn(mockKeyValue)

        `when`(
            mockFactory.create(
                mockContext,
                AWSCognitoLegacyCredentialStore.AWS_MOBILE_CLIENT_PROVIDER
            )
        ).thenReturn(mockKeyValue)

        `when`(mockFactory.create(mockContext, deviceDetailsCacheKey)).thenReturn(mockKeyValue)
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
        `when`(mockKeyValue.get(cachedAccessTokenKey)).thenReturn(DUMMY_TOKEN)
        `when`(mockKeyValue.get(cachedRefreshTokenKey)).thenReturn("refreshToken")
        `when`(mockKeyValue.get(cachedTokenExpirationKey)).thenReturn(EXPIRATION_TIMESTAMP_IN_MS.toString())

        // Device Metadata
        `when`(mockKeyValue.get("DeviceKey")).thenReturn("someDeviceKey")
        `when`(mockKeyValue.get("DeviceGroupKey")).thenReturn("someDeviceGroupKey")
        `when`(mockKeyValue.get("DeviceSecret")).thenReturn("someSecret")

        // AWS Creds
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"accessKey"}")).thenReturn("accessKeyId")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"secretKey"}")).thenReturn("secretAccessKey")
        `when`(mockKeyValue.get("$IDENTITY_POOL_ID.${"sessionToken"}")).thenReturn("sessionToken")
        `when`(
            mockKeyValue.get("$IDENTITY_POOL_ID.${"expirationDate"}")
        ).thenReturn(EXPIRATION_TIMESTAMP_IN_MS.toString())

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
                this.appClientId = APP_CLIENT
            }
        )
    }

    private fun getSRPCredential(): AmplifyCredential = AmplifyCredential.UserAndIdentityPool(
        SignedInData(
            "1234567890",
            "amplify_user",
            Date(0),
            SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            CognitoUserPoolTokens("idToken", DUMMY_TOKEN, "refreshToken", EXPIRATION_TIMESTAMP_IN_SEC)
        ),
        "identityPool",
        AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", EXPIRATION_TIMESTAMP_IN_SEC)
    )

    private fun getDeviceMetaData(): DeviceMetadata = AmplifyCredential.DeviceData(
        DeviceMetadata.Metadata(
            "someDeviceKey",
            "someDeviceGroupKey",
            "someSecret"
        )
    ).deviceMetadata

    private fun getHostedUICredential(): AmplifyCredential = AmplifyCredential.UserAndIdentityPool(
        SignedInData(
            "1234567890",
            "amplify_user",
            Date(0),
            SignInMethod.HostedUI(),
            CognitoUserPoolTokens("idToken", DUMMY_TOKEN, "refreshToken", EXPIRATION_TIMESTAMP_IN_SEC)
        ),
        "identityPool",
        AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", EXPIRATION_TIMESTAMP_IN_SEC)
    )
}
