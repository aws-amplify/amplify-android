package com.amplifyframework.auth.cognito
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CredentialStoreClient
import com.amplifyframework.auth.cognito.RealAWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.isValid


import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.CognitoIdentityException
import aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.forgotPassword
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito.CognitoIdentity
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito.CognitoIdentityProvider
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.ResponseType
//import com.amplifyframework.auth.cognito.featuretest.serializers.AuthStatesProxy


import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.google.gson.Gson
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
import generated.model.*
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.invoke
import io.mockk.verify
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import net.bytebuddy.implementation.InvokeDynamic.lambda
import org.bouncycastle.asn1.x500.style.RFC4519Style
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.json.JSONArray
import org.junit.Test.None
import java.util.Date
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertTrue


class AWSCognitoAuthPluginFeatureTest(private val testCase: generated.model.UnitTest) {
    private val sut = AWSCognitoAuthPlugin()


    private var apiExecutionResult: Any? = null


    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2Mj" +
            "M5MDIyfQ.e4RpZTfAb3oXkfq3IwHtR_8Zhn0U1JDV7McZPlBXyhw"

    private var logger = mockk<Logger>(relaxed = true)

    private var authConfig = mockk<AuthConfiguration> {
        every { userPool } returns UserPoolConfiguration.invoke {
            this.appClientId = "app Client Id"
            this.appClientSecret = "app Client Secret"
            this.pinpointAppId = null
        }
    }

    private val credentials = AmplifyCredential.UserPool(
        SignedInData(
            "userId",
            "username",
            Date(0),
            SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            CognitoUserPoolTokens(dummyToken, dummyToken, dummyToken, 120L)
        )
    )


    private val mockCognitoIPClient = mockk<CognitoIdentityProviderClient>()
    private val mockCognitoIdClient = mockk<CognitoIdentityClient>()
    private val cognitoMockFactory = CognitoMockFactory(mockCognitoIPClient, mockCognitoIdClient, testCase)
    private var authService = mockk<AWSCognitoAuthService> {
        every { cognitoIdentityProviderClient } returns mockCognitoIPClient
    }

    private val expectedEndpointId = "test-endpoint-id"


    private var authEnvironment = mockk<AuthEnvironment> {

        every { context } returns mockk()
        every { configuration } returns authConfig
        every { logger } returns this@AWSCognitoAuthPluginFeatureTest.logger
        every { cognitoAuthService } returns authService
        every { getPinpointEndpointId() } returns expectedEndpointId
    }

    private var currentState: AuthenticationState = AuthenticationState.Configured()

    private var authStateMachine = mockk<AuthStateMachine>(relaxed = true) {
        every { getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(
                mockk {
                    every { authNState } returns currentState
                }
            )
        }
    }


    private fun setUp() {
        this.sut.realPlugin = RealAWSCognitoAuthPlugin(
            this.authConfig,
            this.authEnvironment,
            this.authStateMachine,
            this.logger
        )
        mockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
        every { any<AmplifyCredential>().isValid() } returns true

        // set up user pool
        coEvery { authConfig.userPool } returns UserPoolConfiguration.invoke {
            appClientId = "app Client Id"
            appClientSecret = "app Client Secret"
        }
        coEvery { authEnvironment.getUserContextData(any()) } returns null

        // set up SRP helper
        mockkObject(SRPHelper)
        mockkObject(AuthHelper)
        coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "dummy Hash"


    }


    fun api_feature_test() {
        Dispatchers.setMain(newSingleThreadContext("Main thread"))

        setUp() //initialize plugin and basic mocking
        mockAndroidAPIs() //more mocking


        //set the state below
        if (testCase.preConditions!!.state!!.contains("SignedOut")) {
            currentState = AuthenticationState.SignedOut(mockk())

        }
        else if (testCase.preConditions!!.state!!.contains("SignedIn")) {

            currentState = AuthenticationState.SignedIn(credentials.signedInData, mockk())

        }

        else if (testCase.preConditions!!.state!!.contains("SigningIn")) {
            currentState = AuthenticationState.SigningIn()


        }
        else if (testCase.preConditions!!.state!!.contains("NotConfigured")) {
            currentState = AuthenticationState.NotConfigured()


        }
        //execute smithy specified API on smithy specified parameters
        apiExecutionResult = apiExecutor(sut, testCase.api!!, testCase!!.preConditions!!.mockedResponses!![0].responseType!!)


        // THEN
        if (testCase.preConditions!!.mockedResponses!![0].responseType == TypeResponse.Success) {
            testCase.validations!!.forEach(this::verify) //for success smithy test run verification
        }
        else {
            val apiExecutorException = apiExecutionResult.toStr().substringBefore('{')

            assertEquals(apiExecutorException,
                testCase!!.preConditions!!.mockedResponses!![0].response!!.asError().errorType!!.substringBefore('[')
            ) //for error smithy test ensure exception is equal
        }
        Dispatchers.resetMain()
        clearAllMocks()

    }


    private fun mocking() {
        cognitoMockFactory.mock()

        if (testCase.preConditions!!.mockedResponses!![0].responseType == TypeResponse.Error) {
            every {authService.cognitoIdentityProviderClient} returns mockk()

            every {authConfig.userPool} returns UserPoolConfiguration.invoke { appClientId = null }
        }
        //special mocking


    }

    /**
     * Mock Android APIs as per need basis.
     * This is cheaper than using Robolectric.
     */
    private fun mockAndroidAPIs() {
        mocking()
        mockkObject(AuthHelper)
        coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"

    }

    private fun verify(validation: generated.model.Validation) {

        when (validation.shapetype) {
            generated.model.ShapeType.Cognito -> verifyCognito(validation.shape!!.asCognito())

            generated.model.ShapeType.Amplify -> {
                val apiExecutionResponseClass = getResponse(apiExecutionResult!!)


                //initialize jsonAmplify to jsonObject or jsonArray

                var jsonAmplify = try {
                    JSONObject(validation.shape!!.asAmplify().response!!.asString())
                }
                catch (e : Exception) {
                    try {
                        JSONArray(validation.shape!!.asAmplify().response!!.asString())

                    } catch (e : Exception) {
                        assert(false)
                    }
                }

                if (jsonAmplify is JSONArray) {
                    for (i in 0 until jsonAmplify.length()) {
                        verifyAmplify(JSONObject(jsonAmplify[i].toStr()))
                        //verify each Json object in the array

                    }
                    return
                }
                verifyAmplify(JSONObject(jsonAmplify.toStr()))

            }

            else -> {
                val getStateLatch = CountDownLatch(1)
                authStateMachine.getCurrentState { authState ->
                    assertEquals(validation.shape!!.asStateMachine().expectedState!!, authState.toStr())
                    getStateLatch.countDown()
                    //ensure smithy expected state = environment state
                }
                getStateLatch.await(10, TimeUnit.SECONDS)
            }
        }
    }

    private fun verifyAmplify(jsonAmplify: JSONObject) {

        val propertiesOfAPIResponseClass = apiExecutionResult!!::class.declaredMemberProperties
        // Access the private property values of the api execution result
        propertiesOfAPIResponseClass.forEach { property ->
            // Mark the property as accessible so we can read its value
            property.isAccessible = true

            val name = property.name.toStr()  //name of private variable of api result


            val functionsAPIResponse = apiExecutionResult!!::class.functions //get list of functions in the class

            // Find the function with the given name since this function retrieves value of property
            val function = functionsAPIResponse.find {
                it.name == name || it.name == "get${name[0].uppercaseChar()}${name.substring(1)}"

            }
            val functionReturn = function?.call(apiExecutionResult).toStr() //call function to get value

            if (functionReturn != null) {
                var smithyCompareResponse = jsonAmplify[name].toStr()
                if (smithyCompareResponse[0] !== '{') {
                    assertEquals(smithyCompareResponse, functionReturn) //compare smithy value to apiexec value
                }
                else {
                    smithyCompareResponse = smithyCompareResponse.substringAfter('{')

                    var resultObjectHelper = functionReturn.substring(functionReturn.indexOf('{'))
                    resultObjectHelper = resultObjectHelper.replace("=", ":")

                    val keys = JSONObject(resultObjectHelper).keys()
                    for (value in keys) {
                        assertEquals(JSONObject(smithyCompareResponse)[value.toStr()].toStr(),
                            JSONObject(resultObjectHelper)[value.toStr()].toStr())
                        //compare smithy json value to apiexec json value

                    }
                }
            }
        }
    }


    private fun verifyCognito(validation: generated.model.Cognito) {

        var expectedRequest =
            CognitoRequestFactory.getExpectedRequestFor(validation.apiName!!, validation!!.request!!.asMap())

        var apiName = validation.apiName
        coVerify {
            when (validation.type) {
                is CognitoType.Cognitoidentityprovider -> Pair(mockCognitoIPClient, mockCognitoIPClient::class)
                else -> Pair(mockCognitoIdClient, mockCognitoIPClient::class)
            }.apply {
                second.declaredFunctions.first {

                    it.name == apiName
                }.callSuspend(first, expectedRequest)
            //ensure captured request on plugin in apiexec step == smithy request
            }
        }

    }

}
fun getResponse(apiExecResult : Any): Any = when (apiExecResult) {
    is AuthSignInResult -> apiExecResult as AuthSignInResult
    is AuthSignUpResult -> apiExecResult as AuthSignUpResult

    is AuthResetPasswordResult -> apiExecResult as AuthResetPasswordResult
    is AuthUpdateAttributeResult -> apiExecResult as AuthUpdateAttributeResult
    is FederateToIdentityPoolResult -> apiExecResult as FederateToIdentityPoolResult
    is AuthSessionResult<*> -> apiExecResult as AuthSessionResult<AuthSession> //for fetchAuthSession

    else -> {}
}
