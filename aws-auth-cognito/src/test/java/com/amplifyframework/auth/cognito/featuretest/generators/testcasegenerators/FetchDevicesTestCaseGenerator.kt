package com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceType
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.exceptions.SignedOutException
import kotlinx.serialization.json.JsonObject

object FetchDevicesTestCaseGenerator : SerializableProvider {

    private val expectedSuccess = listOf<AuthDevice>(AuthDevice.fromId("deviceKey")).toJsonElement()

    private val mockCognitoResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "listDevices",
        ResponseType.Success,
        mapOf(
            "devices" to listOf<DeviceType>(
                DeviceType.invoke {
                    deviceAttributes = listOf<AttributeType>(
                        AttributeType.invoke {
                            name = "name"
                            value = "value"
                        }
                    )
                    deviceKey = "deviceKey"
                    deviceCreateDate = Instant.now()
                    deviceLastAuthenticatedDate = Instant.now()
                    deviceLastModifiedDate = Instant.now()
                }
            )
        ).toJsonElement()
    )

    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchDevices,
        ResponseType.Success,
        expectedSuccess,
    )

    private val baseCase = FeatureTestCase(
        description = "Test that Cognito is called with given payload and returns successful data",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(mockCognitoResponse)
        ),
        api = API(
            AuthAPI.fetchDevices,
            JsonObject(emptyMap()),
            JsonObject(emptyMap()),
        ),
        validations = listOf(apiReturnValidation)
    )

    private val successCase: FeatureTestCase = baseCase.copy(
        description = "List of devices returned when fetch devices API succeeds",
        preConditions = baseCase.preConditions.copy(mockedResponses = listOf(mockCognitoResponse)),
        validations = baseCase.validations.plus(apiReturnValidation)
    )

    private val errorCase: FeatureTestCase
        get() {
            val errorResponse = SignedOutException()
            return baseCase.copy(
                description = "AuthException is thrown when forgetDevice API is called without signing in",
                preConditions = baseCase.preConditions.copy(
                    state = "SignedOut_Configured.json",
                    mockedResponses = listOf(
                        MockResponse(
                            CognitoType.CognitoIdentityProvider,
                            "forgetDevice",
                            ResponseType.Failure,
                            errorResponse.toJsonElement()
                        )
                    )
                ),
                validations = listOf(
                    ExpectationShapes.Amplify(
                        AuthAPI.forgetDevice,
                        ResponseType.Failure,
                        com.amplifyframework.auth.exceptions.SignedOutException().toJsonElement(),
                    )
                )
            )
        }

    override val serializables: List<Any> = listOf(baseCase, errorCase, successCase)
}
