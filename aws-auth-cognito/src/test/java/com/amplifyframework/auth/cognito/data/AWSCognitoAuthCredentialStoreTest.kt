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
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import java.util.Date
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AWSCognitoAuthCredentialStoreTest {

    companion object {
        private const val IDENTITY_POOL_ID: String = "identityPoolID"
        private const val USER_POOL_ID: String = "userPoolID"
        private const val KEY_WITH_IDENTITY_POOL: String = "amplify.$IDENTITY_POOL_ID.session"
        private const val KEY_WITH_USER_POOL: String = "amplify.$USER_POOL_ID.session"
        private const val KEY_WITH_USER_AND_IDENTITY_POOL: String = "amplify.$USER_POOL_ID.$IDENTITY_POOL_ID.session"
    }

    private val keyValueRepoID: String = "com.amplify.credentialStore"

    @Mock
    private lateinit var mockConfig: AuthConfiguration

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockKeyValue: KeyValueRepository

    @Mock
    private lateinit var mockFactory: KeyValueRepositoryFactory

    private lateinit var persistentStore: AWSCognitoAuthCredentialStore

    @Before
    fun setup() {
        Mockito.`when`(
            mockFactory.create(
                mockContext,
                keyValueRepoID,
                true,
            )
        ).thenReturn(mockKeyValue)

        Mockito.`when`(mockKeyValue.get(Mockito.anyString())).thenReturn(
            serialized(getCredential())
        )
    }

    @Test
    fun testSaveCredentialWithUserPool() {
        setupUserPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
        persistentStore.saveCredential(getCredential())
        verify(mockKeyValue, times(1))
            .put(KEY_WITH_USER_POOL, serialized(getCredential()))
    }

    @Test
    fun testSaveCredentialWithIdentityPool() {
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)

        persistentStore.saveCredential(getCredential())

        verify(mockKeyValue, times(1))
            .put(KEY_WITH_IDENTITY_POOL, serialized(getCredential()))
    }

    @Test
    fun testSaveCredentialWithUserAndIdentityPool() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)

        persistentStore.saveCredential(getCredential())

        verify(mockKeyValue, times(1))
            .put(KEY_WITH_USER_AND_IDENTITY_POOL, serialized(getCredential()))
    }

    @Test
    fun testRetrieveCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)

        val actual = persistentStore.retrieveCredential()

        Assert.assertEquals(actual, getCredential())
    }

    @Test
    fun testDeleteCredential() {
        setupUserPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)

        persistentStore.deleteCredential()

        verify(mockKeyValue, times(1)).remove(KEY_WITH_USER_POOL)
    }

    @Test
    fun testInMemoryCredentialStore() {
        val store = AWSCognitoAuthCredentialStore(mockContext, mockConfig, false)

        store.saveCredential(getCredential())
        assertEquals(getCredential(), store.retrieveCredential())

        store.deleteCredential()
        assertEquals(AmplifyCredential.Empty, store.retrieveCredential())
    }

    @Test
    @Ignore("fix as per new store format")
    fun testCognitoUserPoolTokensIsReturnedAsNullIfAllItsFieldsAreNull() {
        val credential = getCredential()

        setStoreCredentials(credential)

        val actual = persistentStore.retrieveCredential()

        Assert.assertEquals(AmplifyCredential.Empty, actual)
    }

    @Test
    @Ignore("fix as per new store format")
    fun testAWSCredentialsIsReturnedAsNullIfAllItsFieldsAreNull() {
        val credential = getCredential()

        setStoreCredentials(credential)

        val actual = persistentStore.retrieveCredential()

        Assert.assertEquals(AmplifyCredential.Empty, actual)
    }

    private fun setStoreCredentials(credential: AmplifyCredential) {
        Mockito.`when`(mockKeyValue.get(Mockito.anyString())).thenReturn(serialized(credential))

        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
    }

    private fun setupIdentityPoolConfig() {
        Mockito.`when`(mockConfig.identityPool).thenReturn(
            IdentityPoolConfiguration {
                this.poolId = IDENTITY_POOL_ID
            }
        )
    }

    private fun setupUserPoolConfig() {
        Mockito.`when`(mockConfig.userPool).thenReturn(
            UserPoolConfiguration {
                this.poolId = USER_POOL_ID
                this.appClientId = "appClientId"
            }
        )
    }

    private fun getCredential(): AmplifyCredential {
        val expiration = 123123L
        return AmplifyCredential.UserAndIdentityPool(
            SignedInData(
                "userId",
                "username",
                Date(0),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", expiration),
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
