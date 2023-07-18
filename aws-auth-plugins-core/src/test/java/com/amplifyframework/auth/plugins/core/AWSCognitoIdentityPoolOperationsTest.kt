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

package com.amplifyframework.auth.plugins.core

import android.content.Context
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.plugins.core.data.AWSCognitoIdentityPoolConfiguration
import com.amplifyframework.auth.plugins.core.data.AWSCredentialsInternal
import com.amplifyframework.auth.plugins.core.data.AuthCredentialStore
import com.amplifyframework.auth.result.AuthSessionResult
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class AWSCognitoIdentityPoolOperationsTest {
    private val config = AWSCognitoIdentityPoolConfiguration("poolId")
    private val KEY_LOGINS_PROVIDER = "amplify.${config.poolId}.session.loginsProvider"
    private val KEY_IDENTITY_ID = "amplify.${config.poolId}.session.identityId"
    private val KEY_AWS_CREDENTIALS = "amplify.${config.poolId}.session.credential"

    private val context = mockk<Context>(relaxed = true)
    private val mockCognitoIDClient = mockk<CognitoIdentityClient>()
    private lateinit var awsCognitoIdentityPoolOperations: AWSCognitoIdentityPoolOperations

    @Before
    fun setup() {
        mockkObject(CognitoClientFactory)
        coEvery { CognitoClientFactory.createIdentityClient(config, any(), any()) } returns mockCognitoIDClient
        mockkConstructor(AuthCredentialStore::class)
        coEvery { anyConstructed<AuthCredentialStore>().put(any(), any()) } returns Unit

        awsCognitoIdentityPoolOperations = AWSCognitoIdentityPoolOperations(context, config, "test-plugin", "0")
    }

    @Test
    fun testFetchAWSCognitoIdentityPoolDetailsWithoutCredsSucceeds() = runBlocking {
        coEvery { anyConstructed<AuthCredentialStore>().get(any()) } returns null

        val logins = listOf(LoginProvider("test-provider", "test-id-token"))
        val expected = AWSCognitoIdentityPoolDetails(
            AuthSessionResult.success("test-identity-id"),
            AuthSessionResult.success(AWSCredentials.createAWSCredentials("accessKey", "secretKey", "session", 0))
        )

        coEvery { mockCognitoIDClient.getId(any()) } returns GetIdResponse.invoke {
            identityId = "test-identity-id"
        }
        coEvery { mockCognitoIDClient.getCredentialsForIdentity(any()) } returns
            GetCredentialsForIdentityResponse.invoke {
                identityId = "test-identity-id"
                credentials = Credentials.invoke {
                    accessKeyId = "accessKey"
                    secretKey = "secretKey"
                    sessionToken = "session"
                    expiration = Instant.fromEpochSeconds(0)
                }
            }

        val result = awsCognitoIdentityPoolOperations.fetchAWSCognitoIdentityPoolDetails(logins, false)
        assertEquals(expected, result)
    }

    @Test
    fun testFetchAWSCognitoIdentityPoolDetailsWithCredsSucceeds() = runBlocking {
        val logins = listOf(LoginProvider("test-provider", "test-id-token"))
        val instant = Instant.now().epochSeconds + 1000L

        coEvery { anyConstructed<AuthCredentialStore>().get(KEY_LOGINS_PROVIDER) } returns Json.encodeToString(
            logins
        )
        coEvery { anyConstructed<AuthCredentialStore>().get(KEY_IDENTITY_ID) } returns "test-identity-id"
        coEvery { anyConstructed<AuthCredentialStore>().get(KEY_AWS_CREDENTIALS) } returns Json.encodeToString(
            AWSCredentialsInternal("accessKey", "secretKey", "session", instant)
        )

        val expected = AWSCognitoIdentityPoolDetails(
            AuthSessionResult.success("test-identity-id"),
            AuthSessionResult.success(AWSCredentials.createAWSCredentials("accessKey", "secretKey", "session", instant))
        )

        val result = awsCognitoIdentityPoolOperations.fetchAWSCognitoIdentityPoolDetails(logins, false)
        assertEquals(expected, result)
    }

    @Test
    fun testFetchAWSCognitoIdentityPoolDetailsWithCredsExpiredSucceeds() = runBlocking {
        val logins = listOf(LoginProvider("test-provider", "test-id-token"))

        coEvery { anyConstructed<AuthCredentialStore>().get(KEY_LOGINS_PROVIDER) } returns Json.encodeToString(
            logins
        )
        coEvery { anyConstructed<AuthCredentialStore>().get(KEY_IDENTITY_ID) } returns "test-identity-id"
        coEvery { anyConstructed<AuthCredentialStore>().get(KEY_AWS_CREDENTIALS) } returns Json.encodeToString(
            AWSCredentialsInternal("accessKey", "secretKey", "session", 0)
        )

        val expected = AWSCognitoIdentityPoolDetails(
            AuthSessionResult.success("test-identity-id"),
            AuthSessionResult.success(AWSCredentials.createAWSCredentials("accessKey", "secretKey", "session", 0))
        )

        coEvery { mockCognitoIDClient.getCredentialsForIdentity(any()) } returns
            GetCredentialsForIdentityResponse.invoke {
                identityId = "test-identity-id"
                credentials = Credentials.invoke {
                    accessKeyId = "accessKey"
                    secretKey = "secretKey"
                    sessionToken = "session"
                    expiration = Instant.fromEpochSeconds(0)
                }
            }

        val result = awsCognitoIdentityPoolOperations.fetchAWSCognitoIdentityPoolDetails(logins, false)
        assertEquals(expected, result)
    }

    @Test
    fun testFetchAWSCognitoIdentityPoolDetailsWithoutCredsFails() = runBlocking {
        coEvery { anyConstructed<AuthCredentialStore>().get(any()) } returns null

        val serviceException = aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException { }
        val expectedException = NotAuthorizedException(
            recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED,
            cause = serviceException
        )

        val logins = listOf(LoginProvider("test-provider", "test-id-token"))
        val expected = AWSCognitoIdentityPoolDetails(
            AuthSessionResult.failure(expectedException),
            AuthSessionResult.failure(expectedException)
        )

        coEvery { mockCognitoIDClient.getId(any()) } throws(serviceException)

        val result = awsCognitoIdentityPoolOperations.fetchAWSCognitoIdentityPoolDetails(logins, false)
        assertEquals(expected, result)
    }
}
