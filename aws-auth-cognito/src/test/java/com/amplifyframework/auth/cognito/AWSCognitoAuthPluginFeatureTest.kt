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

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.testutils.featuretest.API
import com.amplifyframework.testutils.featuretest.ExpectationShapes
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import com.amplifyframework.testutils.featuretest.ResponseType.Complete
import com.amplifyframework.testutils.featuretest.ResponseType.Success
import com.amplifyframework.testutils.featuretest.auth.generators.toJsonElement
import com.amplifyframework.testutils.featuretest.auth.serializers.deserializeToAuthState
import com.google.gson.Gson
import featureTest.utilities.APICaptorFactory
import featureTest.utilities.AuthOptionsFactory
import featureTest.utilities.CognitoMockFactory
import featureTest.utilities.CognitoRequestFactory.getExpectedRequestFor
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AWSCognitoAuthPluginFeatureTest(private val fileName: String) {

    lateinit var feature: FeatureTestCase
    lateinit var latch: CountDownLatch

    val sut = AWSCognitoAuthPlugin()
    private lateinit var authStateMachine: AuthStateMachine

    private val mockCognitoIPClient = mockk<CognitoIdentityProviderClient>()
    private val cognitoMockFactory = CognitoMockFactory(mockCognitoIPClient)

    // Used to execute a test in situations where the platform Main dispatcher is not available
    // see [https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/]
    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    companion object {
        private const val testSuiteBasePath = "/feature-test/testsuites"
        private const val statesFilesBasePath = "/feature-test/states"
        private const val configurationFilesBasePath = "/feature-test/configuration"

        @JvmStatic
        @Parameterized.Parameters(name = "test file : {0}")
        fun data(): Collection<String> {
            val resourceDir = File(this::class.java.getResource(testSuiteBasePath)?.file!!)
            assert(resourceDir.isDirectory)

            return resourceDir.walkTopDown()
                .filterNot { it.isDirectory }.map {
                    it.toRelativeString(resourceDir)
                }.toList()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        feature = readTestFeature(fileName)
        sut.realPlugin = readconfiguration(feature.preConditions.`amplify-configuration`)
        latch = CountDownLatch(1)
    }

    @Test
    fun api_feature_test() {
        // GIVEN
        setupMocks()

        // WHEN
        callAPI(feature.api)

        // THEN
        latch.await()
        feature.validations.forEach(this::verify)
    }

    private fun readTestFeature(fileName: String): FeatureTestCase {
        val testCaseFile = this::class.java.getResource("$testSuiteBasePath/$fileName")
        feature = Json.decodeFromString(File(testCaseFile!!.toURI()).readText())
        return feature
    }

    private fun readconfiguration(configuration: String): RealAWSCognitoAuthPlugin {
        val configFileUrl = this::class.java.getResource("$configurationFilesBasePath/$configuration")
        val configJSONObject =
            JSONObject(File(configFileUrl!!.file).readText())
                .getJSONObject("auth")
                .getJSONObject("plugins")
                .getJSONObject("awsCognitoAuthPlugin")
        val authConfiguration = AuthConfiguration.fromJson(configJSONObject)

        val authService = mockk<AWSCognitoAuthServiceBehavior> {
            every { cognitoIdentityProviderClient } returns mockCognitoIPClient
        }

        val authEnvironment = AuthEnvironment(authConfiguration, authService, null, null, logger = mockk())

        authStateMachine = AuthStateMachine(authEnvironment, getState(feature.preConditions.state))

        val credentialStoreStateMachine = mockk<CredentialStoreStateMachine>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)

        return RealAWSCognitoAuthPlugin(
            authConfiguration,
            authEnvironment,
            authStateMachine,
            credentialStoreStateMachine,
            logger
        )
    }

    private fun setupMocks() {
        feature.preConditions.mockedResponses.forEach(cognitoMockFactory::mock)

        feature.validations.filterIsInstance<ExpectationShapes.Amplify>().filter {
            it.apiName == feature.api.name
        }.forEach {
            APICaptorFactory(it, latch)
        }
    }

    private fun verify(validation: ExpectationShapes) {
        when (validation) {
            is ExpectationShapes.Cognito -> {
                val actualResult = cognitoMockFactory.getActualResultFor(validation.apiName)

                assertNotNull(actualResult)
                assertEquals(getExpectedRequestFor(validation), actualResult)
            }
            is ExpectationShapes.Amplify -> {
                val expectedResponse: Any
                val actualResult = if (validation.responseType == Success) {
                    APICaptorFactory.successCaptors[validation.apiName]?.captured.apply {
                        expectedResponse = Gson().fromJson(validation.response.toString(), this?.javaClass)
                    }
                } else if (validation.responseType == Complete) {
                    APICaptorFactory.completeCaptors[validation.apiName]?.captured.toJsonElement().apply {
                        expectedResponse = validation.response as JsonObject
                    }
                } else {
                    APICaptorFactory.errorCaptor.captured.toJsonElement().apply {
                        expectedResponse = validation.response as JsonObject
                    }
                }

                assertNotNull(actualResult)
                assertEquals(expectedResponse, actualResult)
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

    private fun callAPI(api: API) {
        val targetApi = sut::class.memberFunctions.find { it.name == api.name.name }
        val params = api.params as JsonObject
        val requiredParams: MutableMap<KParameter, Any?>? = targetApi?.parameters?.associate {
            it to (params[it.name] as? JsonPrimitive)?.content
        }?.toMutableMap()
        requiredParams?.set(targetApi.parameters[0], sut)

        requiredParams?.let {
            targetApi.parameters.firstOrNull { it.name == "onSuccess" }?.let { onSuccess ->
                it[onSuccess] = APICaptorFactory.onSuccess[api.name]
            }
            targetApi.parameters.firstOrNull { it.name == "onComplete" }?.let { onComplete ->
                it[onComplete] = APICaptorFactory.onComplete[api.name]
            }
            targetApi.parameters.firstOrNull { it.name == "onError" }?.let { onError ->
                it[onError] = APICaptorFactory.onError
            }
        }

        val optionsObj = AuthOptionsFactory.create(api.name, api.options as JsonObject)
        requiredParams?.set(targetApi.parameters.first { it.name == "options" }, optionsObj)

        runBlocking {
            requiredParams?.let { targetApi.callBy(it) }
        }
    }

    private fun getState(state: String): AuthState {
        val stateFileUrl = this::class.java.getResource("$statesFilesBasePath/$state")
        return File(stateFileUrl!!.file).readText().deserializeToAuthState()
    }
}
