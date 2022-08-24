package com.amplifyframework.testutils.featuretest.auth.generators.testcasegenerators

import com.amplifyframework.testutils.featuretest.API
import com.amplifyframework.testutils.featuretest.ExpectationShapes
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import com.amplifyframework.testutils.featuretest.MockResponse
import com.amplifyframework.testutils.featuretest.PreConditions
import com.amplifyframework.testutils.featuretest.ResponseType
import com.amplifyframework.testutils.featuretest.auth.AuthAPI
import com.amplifyframework.testutils.featuretest.auth.generators.exportJson
import com.amplifyframework.testutils.featuretest.auth.generators.toJsonElement
import kotlinx.serialization.json.JsonObject

object ResetPasswordTestCaseGenerator {
    private val mockCognitoResponse = MockResponse(
        "cognito",
        "forgotPassword",
        ResponseType.Success,
        mapOf(
            "codeDeliveryDetails" to mapOf(
                "destination" to "dummy destination",
                "deliveryMedium" to "EMAIL",
                "attributeName" to "dummy attribute"
            )
        ).toJsonElement()
    )

    private val codeDeliveryDetails = mapOf(
        "destination" to "dummy destination",
        "deliveryMedium" to "EMAIL",
        "attributeName" to "dummy attribute"
    )

    private val expectedSuccess =
        mapOf(
            "isPasswordReset" to false,
            "nextStep" to
                    mapOf(
                        "resetPasswordStep" to "CONFIRM_RESET_PASSWORD_WITH_CODE",
                        "additionalInfo" to emptyMap<String, String>(),
                        "codeDeliveryDetails" to codeDeliveryDetails
                    )
        ).toJsonElement()

    private val cognitoValidation = ExpectationShapes.Cognito(
        "forgotPassword",
        mapOf(
            "username" to "someUsername",
            "clientId" to "testAppClientId",
            "clientMetadata" to emptyMap<String, String>()
        ).toJsonElement()
    )

    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.resetPassword,
        ResponseType.Success,
        expectedSuccess,
    )
    private val finalStateValidation = ExpectationShapes.State("AuthenticationState_SignedIn.json")

    private val baseCase = FeatureTestCase(
        description = "Test that Cognito is called with given payload and returns successful data",
        preConditions = PreConditions(
            "authconfiguration.json",
            "AuthenticationState_SignedIn.json",
            mockedResponses = listOf()
        ),
        api = API(
            AuthAPI.resetPassword,
            mapOf("username" to "someUsername").toJsonElement(),
            JsonObject(emptyMap())
        ),
        validations = listOf(cognitoValidation, apiReturnValidation)
    )


    fun AuthResetPasswordResult_object_is_returned_when_reset_password_succeeds() {
        baseCase.copy(
            description = "AuthResetPasswordResult object is returned when reset password succeeds",
            preConditions = baseCase.preConditions.copy(mockedResponses = listOf(mockCognitoResponse)),
            validations = baseCase.validations.plus(apiReturnValidation)
        ).exportJson()
    }

    fun AuthException_is_thrown_when_forgotPassword_API_call_fails() {
        baseCase.copy(
            description = "AuthException is thrown when forgotPassword API call fails",
            preConditions = baseCase.preConditions.copy(
                mockedResponses = listOf(
                    MockResponse(
                        "cognito",
                        "forgotPassword",
                        ResponseType.Failure,
                        mapOf(
                            "message" to "Some cognito error message",
                            "codeDeliveryDetails" to mapOf<String, String>()
                        ).toJsonElement()
                    )
                )
            ),
            validations = listOf(
                ExpectationShapes.Amplify(
                    AuthAPI.resetPassword,
                    ResponseType.Failure,
                    mapOf(
                        "message" to "Some cognito error message"
                    ).toJsonElement(),
                )
            )
        ).exportJson()
    }

}

fun main() {
    ResetPasswordTestCaseGenerator.AuthResetPasswordResult_object_is_returned_when_reset_password_succeeds()
}

