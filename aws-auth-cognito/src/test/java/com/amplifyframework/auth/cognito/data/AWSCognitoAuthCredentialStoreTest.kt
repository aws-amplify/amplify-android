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
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import java.util.Date
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test

class AWSCognitoAuthCredentialStoreTest {

    companion object {
        private const val IDENTITY_POOL_ID: String = "identityPoolID"
        private const val USER_POOL_ID: String = "userPoolID"
        private const val KEY_WITH_IDENTITY_POOL: String = "amplify.$IDENTITY_POOL_ID.session"
        private const val KEY_WITH_USER_POOL: String = "amplify.$USER_POOL_ID.session"
        private const val KEY_WITH_USER_AND_IDENTITY_POOL: String = "amplify.$USER_POOL_ID.$IDENTITY_POOL_ID.session"
    }

    private val mockConfig = mockk<AuthConfiguration>()
    private val mockContext = mockk<Context>()
    private val mockKeyValue = mockk<EncryptedKeyValueRepository>(relaxed = true)

    private lateinit var persistentStore: AWSCognitoAuthCredentialStore

    @Before
    fun setup() {
        mockkConstructor(EncryptedKeyValueRepository::class)
        every { mockKeyValue.get(any<String>()) } returns serialized(getCredential())
    }

    @After
    fun tearDown() {
        unmockkConstructor(EncryptedKeyValueRepository::class)
    }

    @Test
    fun testSaveCredentialWithUserPool() {
        setupUserPoolConfig()
        every { mockConfig.identityPool } returns null
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, mockKeyValue)

        persistentStore.saveCredential(getCredential())

        verify(exactly = 1) { mockKeyValue.put(KEY_WITH_USER_POOL, serialized(getCredential())) }
    }

    @Test
    fun testSaveCredentialWithIdentityPool() {
        setupIdentityPoolConfig()
        every { mockConfig.userPool } returns null
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, mockKeyValue)

        persistentStore.saveCredential(getCredential())

        verify(exactly = 1) { mockKeyValue.put(KEY_WITH_IDENTITY_POOL, serialized(getCredential())) }
    }

    @Test
    fun testSaveCredentialWithUserAndIdentityPool() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, mockKeyValue)

        persistentStore.saveCredential(getCredential())

        verify(exactly = 1) { mockKeyValue.put(KEY_WITH_USER_AND_IDENTITY_POOL, serialized(getCredential())) }
    }

    @Test
    fun testRetrieveCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, mockKeyValue)

        val actual = persistentStore.retrieveCredential()

        actual shouldBeEqual getCredential()
    }

    @Test
    fun testDeleteCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, mockKeyValue)

        persistentStore.deleteCredential()

        verify(exactly = 1) { mockKeyValue.remove(KEY_WITH_USER_AND_IDENTITY_POOL) }
    }

    private fun setupIdentityPoolConfig() {
        every { mockConfig.identityPool } returns IdentityPoolConfiguration {
            this.poolId = IDENTITY_POOL_ID
        }
    }

    private fun setupUserPoolConfig() {
        every { mockConfig.userPool } returns UserPoolConfiguration {
            this.poolId = USER_POOL_ID
            this.appClientId = "appClientId"
        }
    }

    private fun getCredential(): AmplifyCredential {
        val expiration = 123123L
        return AmplifyCredential.UserAndIdentityPool(
            SignedInData(
                "userId",
                "username",
                Date(0),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", expiration)
            ),
            "identityPool",
            AWSCredentials(
                "accessKeyId",
                "secretAccessKey",
                "sessionToken",
                expiration
            )
        )
    }

    private fun serialized(credential: AmplifyCredential): String {
        return Json.encodeToString(credential)
    }
}
