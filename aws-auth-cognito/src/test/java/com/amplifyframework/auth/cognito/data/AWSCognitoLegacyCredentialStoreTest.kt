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
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import java.util.Date
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test

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

        private const val expirationTimestampInSec: Long = 1714431706
        private const val expirationTimestampInMs: Long = 1714431706486
    }

    private val mockConfig = mockk<AuthConfiguration>()
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockKeyValue = mockk<LegacyKeyValueRepository>()

    private lateinit var persistentStore: AuthCredentialStore

    @Before
    fun setup() {
        mockkConstructor(LegacyKeyValueRepository::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(LegacyKeyValueRepository::class)
    }

    @Test
    fun testRetrieveSRPCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig)

        val actual = persistentStore.retrieveCredential()

        actual shouldBeEqual getSRPCredential()
    }

    @Test
    fun testRetrieveDeviceMetaData() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig)

        val actual = persistentStore.retrieveDeviceMetadata("username")

        actual shouldBeEqual getDeviceMetaData()
    }

    @Test
    fun testRetrieveHostedUICredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        setupKeyValueGetters()
        every { anyConstructed<LegacyKeyValueRepository>().get("signInMode") } returns "2"
        persistentStore = AWSCognitoLegacyCredentialStore(mockContext, mockConfig)

        val actual = persistentStore.retrieveCredential()

        actual shouldBeEqual getHostedUICredential()
    }

    private fun setupKeyValueGetters() {
        // Tokens
        every { anyConstructed<LegacyKeyValueRepository>().get(userIdTokenKey) } returns "username"
        every { anyConstructed<LegacyKeyValueRepository>().get(cachedIdTokenKey) } returns "idToken"
        every { anyConstructed<LegacyKeyValueRepository>().get(cachedAccessTokenKey) } returns dummyToken
        every { anyConstructed<LegacyKeyValueRepository>().get(cachedRefreshTokenKey) } returns "refreshToken"
        every {
            anyConstructed<LegacyKeyValueRepository>().get(cachedTokenExpirationKey)
        } returns expirationTimestampInMs.toString()

        // Device Metadata
        every { anyConstructed<LegacyKeyValueRepository>().get("DeviceKey") } returns "someDeviceKey"
        every { anyConstructed<LegacyKeyValueRepository>().get("DeviceGroupKey") } returns "someDeviceGroupKey"
        every { anyConstructed<LegacyKeyValueRepository>().get("DeviceSecret") } returns "someSecret"

        // AWS Creds
        every {
            anyConstructed<LegacyKeyValueRepository>().get("$IDENTITY_POOL_ID.accessKey")
        } returns "accessKeyId"
        every {
            anyConstructed<LegacyKeyValueRepository>().get("$IDENTITY_POOL_ID.secretKey")
        } returns "secretAccessKey"
        every {
            anyConstructed<LegacyKeyValueRepository>().get("$IDENTITY_POOL_ID.sessionToken")
        } returns "sessionToken"
        every {
            anyConstructed<LegacyKeyValueRepository>().get("$IDENTITY_POOL_ID.expirationDate")
        } returns expirationTimestampInMs.toString()

        // Identity ID
        every {
            anyConstructed<LegacyKeyValueRepository>().get("$IDENTITY_POOL_ID.identityId")
        } returns "identityPool"

        // Mobile Client SignInMethod
        every { anyConstructed<LegacyKeyValueRepository>().get("signInMode") } returns null
        every { anyConstructed<LegacyKeyValueRepository>().get("provider") } returns null
    }

    private fun setupIdentityPoolConfig() {
        every { mockConfig.identityPool } returns IdentityPoolConfiguration {
            this.poolId = IDENTITY_POOL_ID
        }
    }

    private fun setupUserPoolConfig() {
        every { mockConfig.userPool } returns UserPoolConfiguration {
            this.poolId = USER_POOL_ID
            this.appClientId = appClient
        }
    }

    private fun getSRPCredential(): AmplifyCredential {
        return AmplifyCredential.UserAndIdentityPool(
            SignedInData(
                "1234567890",
                "amplify_user",
                Date(0),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                CognitoUserPoolTokens("idToken", dummyToken, "refreshToken", expirationTimestampInSec)
            ),
            "identityPool",
            AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", expirationTimestampInSec)
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
                CognitoUserPoolTokens("idToken", dummyToken, "refreshToken", expirationTimestampInSec)
            ),
            "identityPool",
            AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", expirationTimestampInSec)
        )
    }
}
