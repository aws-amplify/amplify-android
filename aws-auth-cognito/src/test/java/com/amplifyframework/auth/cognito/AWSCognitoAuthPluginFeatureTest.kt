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

package com.amplifyframework.auth.cognito

import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito.CognitoIdentity
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito.CognitoIdentityProvider
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.cognito.featuretest.serializers.deserializeToAuthState
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.states.AuthState
import featureTest.utilities.CognitoMockFactory
import featureTest.utilities.CognitoRequestFactory
import featureTest.utilities.apiExecutor
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AWSCognitoAuthPluginFeatureTest(private val testCase: FeatureTestCase) {

    lateinit var feature: FeatureTestCase
    private var apiExecutionResult: Any? = null

    private val sut = AWSCognitoAuthPlugin()
    private lateinit var authStateMachine: AuthStateMachine

    private val mockCognitoIPClient = mockk<CognitoIdentityProviderClient>(relaxed = true)
    private val mockCognitoIdClient = mockk<CognitoIdentityClient>()
    private val cognitoMockFactory = CognitoMockFactory(mockCognitoIPClient, mockCognitoIdClient)

    // Used to execute a test in situations where the platform Main dispatcher is not available
    // see [https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/]
    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    companion object {
        private const val testSuiteBasePath = "/feature-test/testsuites"
        private const val statesFilesBasePath = "/feature-test/states"
        private const val configurationFilesBasePath = "/feature-test/configuration"

        private val apisToSkip: List<AuthAPI> = listOf()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<FeatureTestCase> {
            val resourceDir = File(this::class.java.getResource(testSuiteBasePath)?.file!!)
            assert(resourceDir.isDirectory)

            return resourceDir.walkTopDown()
                .filterNot { it.isDirectory }.map {
                    it.toRelativeString(resourceDir)
                }.map {
                    readTestFeature(it)
                }.toList().filterNot {
                    it.api.name in apisToSkip
                }
        }

        private fun readTestFeature(fileName: String): FeatureTestCase {
            val testCaseFile = this::class.java.getResource("$testSuiteBasePath/$fileName")
            return Json.decodeFromString(File(testCaseFile!!.toURI()).readText())
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        feature = testCase
        sut.realPlugin = readConfiguration(feature.preConditions.`amplify-configuration`)
    }

    @Test
    fun api_feature_test() {
        // GIVEN
        mockAndroidAPIs()
        feature.preConditions.mockedResponses.forEach(cognitoMockFactory::mock)

        // WHEN
        apiExecutionResult = apiExecutor(sut, feature.api)

        // THEN
        feature.validations.forEach(this::verify)
    }

    /**
     * Mock Android APIs as per need basis.
     * This is cheaper than using Robolectric.
     */
    private fun mockAndroidAPIs() {
        mockkObject(AuthHelper)
        coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"
    }

    private fun readConfiguration(configuration: String): RealAWSCognitoAuthPlugin {
        val configFileUrl = this::class.java.getResource("$configurationFilesBasePath/$configuration")
        val configJSONObject =
            JSONObject(File(configFileUrl!!.file).readText())
                .getJSONObject("auth")
                .getJSONObject("plugins")
                .getJSONObject("awsCognitoAuthPlugin")
        val authConfiguration = AuthConfiguration.fromJson(configJSONObject)

        val authService = mockk<AWSCognitoAuthService> {
            every { cognitoIdentityProviderClient } returns mockCognitoIPClient
            every { cognitoIdentityClient } returns mockCognitoIdClient
        }

        /**
         * Always consider amplify credential is valid. This will need to be mocked otherwise
         * when we test the expiration based test cases.
         */
        mockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
        every { any<AmplifyCredential>().isValid() } returns true

        val credentialStoreClient = mockk<CredentialStoreClient>(relaxed = true)
        coEvery { credentialStoreClient.loadCredentials(capture(slot<CredentialType.Device>())) } coAnswers {
            AmplifyCredential.DeviceData(DeviceMetadata.Empty)
        }

        val logger = mockk<Logger>(relaxed = true)

        val authEnvironment = AuthEnvironment(
            mockk(),
            authConfiguration,
            authService,
            credentialStoreClient,
            null,
            null,
            logger
        )

        authStateMachine = AuthStateMachine(authEnvironment, getState(feature.preConditions.state))

        return RealAWSCognitoAuthPlugin(authConfiguration, authEnvironment, authStateMachine, logger)
    }

    private fun verify(validation: ExpectationShapes) {
        when (validation) {
            is Cognito -> verifyCognito(validation)

            is ExpectationShapes.Amplify -> {
                val expectedResponse = validation.response

                assertEquals(expectedResponse, apiExecutionResult.toJsonElement())
            }
            is ExpectationShapes.State -> {
                val getStateLatch = CountDownLatch(1)
                authStateMachine.getCurrentState { authState ->
                    assertEquals(getState(validation.expectedState), authState)
                    getStateLatch.countDown()
                }
                getStateLatch.await(10, TimeUnit.SECONDS)
            }
        }
    }

    private fun getState(state: String): AuthState {
        val stateFileUrl = this::class.java.getResource("$statesFilesBasePath/$state")
        return File(stateFileUrl!!.file).readText().deserializeToAuthState()
    }

    private fun verifyCognito(validation: Cognito) {
        val expectedRequest = CognitoRequestFactory.getExpectedRequestFor(validation)

        coVerify {
            when (validation) {
                is CognitoIdentity -> mockCognitoIdClient to mockCognitoIPClient::class
                is CognitoIdentityProvider -> mockCognitoIPClient to mockCognitoIPClient::class
            }.apply {
                second.declaredFunctions.first {
                    it.name == validation.apiName
                }.callSuspend(first, expectedRequest)
            }
        }
    }
}
