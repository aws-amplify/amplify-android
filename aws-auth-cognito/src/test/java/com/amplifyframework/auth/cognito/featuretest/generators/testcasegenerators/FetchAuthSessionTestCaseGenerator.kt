package com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators

import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amazonaws.auth.CognitoCredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.AWSCognitoUserPoolTokens
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators.AuthStateJsonGenerator
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import kotlinx.serialization.json.JsonObject

object FetchAuthSessionTestCaseGenerator : SerializableProvider {

    private val expectedSuccess = AWSCognitoAuthSession(
        true,
        identityIdResult = AWSCognitoAuthSession.getIdentityIdResult("someIdentityId"),
        awsCredentialsResult = AuthSessionResult.success(com.amplifyframework.auth.AWSCredentials("someid","someid")),
        userSubResult = AuthSessionResult.success("userId"),
        userPoolTokensResult = AuthSessionResult.success(AWSCognitoUserPoolTokens(
            accessToken = AuthStateJsonGenerator.dummyToken,
            idToken = AuthStateJsonGenerator.dummyToken,
            refreshToken = AuthStateJsonGenerator.dummyToken)
        )
    ).toJsonElement()
/*
        mapOf(
            "isSignedIn" to true,
            "identityIdResult" to mapOf("value" to "someid"),
            "awsCredentialsResult" to mapOf("value" to "someid"),
            "userSubResult" to mapOf("value" to "someid"),
            "userPoolTokensResult" to AuthSessionResult.success(
                AWSCognitoUserPoolTokens(
                    accessToken = AuthStateJsonGenerator.dummyToken,
                    idToken = AuthStateJsonGenerator.dummyToken,
                    refreshToken = AuthStateJsonGenerator.dummyToken
                )
            )
        ).toJsonElement()*/


    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchAuthSession,
        ResponseType.Success,
        expectedSuccess,
    )

    private val baseCase = FeatureTestCase(
        description = "Test that API is called with given payload and returns successful data",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf()
        ),
        api = API(
            name = AuthAPI.fetchAuthSession,
            params = JsonObject(emptyMap()),
            JsonObject(emptyMap())
        ),
        validations = listOf(apiReturnValidation)
    )


    private val successCase: FeatureTestCase = baseCase.copy(
        description = "AuthSession object is successfully returned",
        preConditions = baseCase.preConditions,
        validations = baseCase.validations.plus(apiReturnValidation)
    )

    private val refreshSuccessCase: FeatureTestCase = baseCase.copy(
        description = "AuthSession object is successfully returned after refresh",
        preConditions = baseCase.preConditions,
        api = API(
            name = AuthAPI.fetchAuthSession,
            params = JsonObject(emptyMap()),
            options = mapOf("forceRefresh" to true).toJsonElement(),
        ),
        validations = baseCase.validations.plus(apiReturnValidation)
    )
    private val errorCase: FeatureTestCase
        get() {
            val notAuthorizedErrorResponse = NotAuthorizedException.invoke { }
            return baseCase.copy(
                description = "AuthException is thrown when fetchAuthSession API call fails",
                preConditions = baseCase.preConditions.copy(
                    state = "SignedOut_Configured.json"
                ),
                validations = listOf(
                    ExpectationShapes.Amplify(
                        AuthAPI.fetchAuthSession,
                        ResponseType.Failure,
                        com.amplifyframework.auth.exceptions.NotAuthorizedException(
                            cause = notAuthorizedErrorResponse
                        ).toJsonElement()
                    )
                )
            )
        }

    override val serializables: List<Any> = listOf(baseCase, errorCase, successCase, refreshSuccessCase)
}