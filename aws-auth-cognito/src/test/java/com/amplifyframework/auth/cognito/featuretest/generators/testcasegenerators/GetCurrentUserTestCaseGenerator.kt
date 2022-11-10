package com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators

import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import kotlinx.serialization.json.JsonObject

object GetCurrentUserTestCaseGenerator : SerializableProvider {

    private val expectedSuccess = AuthUser("userId", "username").toJsonElement()

    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.getCurrentUser,
        ResponseType.Success,
        expectedSuccess,
    )

    private val baseCase = FeatureTestCase(
        description = "User object is successfully returned",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf()
        ),
        api = API(
            name = AuthAPI.getCurrentUser,
            params = JsonObject(emptyMap()),
            JsonObject(emptyMap())
        ),
        validations = listOf(apiReturnValidation)
    )

    private val errorCase: FeatureTestCase
        get() {
            val errorResponse = NotAuthorizedException.invoke {}
            return baseCase.copy(
                description = "SignedOutException is thrown when user signs out",
                preConditions = baseCase.preConditions.copy(
                    state = "SignedOut_Configured.json"
                ),
                validations = listOf(
                    ExpectationShapes.Amplify(
                        AuthAPI.getCurrentUser,
                        ResponseType.Failure,
                        com.amplifyframework.auth.exceptions.SignedOutException().toJsonElement(),
                    )
                )
            )
        }

    override val serializables: List<Any> = listOf(
        baseCase,
        errorCase
    )
}
