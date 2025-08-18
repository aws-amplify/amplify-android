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

package com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators

import aws.sdk.kotlin.services.cognitoidentity.model.TooManyRequestsException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
import com.amplifyframework.auth.AWSCognitoUserPoolTokens
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
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
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSessionResult
import java.util.TimeZone
import kotlinx.serialization.json.JsonObject

object FetchAuthSessionTestCaseGenerator : SerializableProvider {

    private val initialTimeZone = TimeZone.getDefault()

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("US/Pacific"))
    }

    override fun tearDown() {
        TimeZone.setDefault(initialTimeZone)
    }

    private val mockedRefreshGetTokensFromRefreshTokensResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "getTokensFromRefreshTokens",
        ResponseType.Success,
        mapOf(
            "authenticationResult" to mapOf(
                "idToken" to AuthStateJsonGenerator.DUMMY_TOKEN_2,
                "accessToken" to AuthStateJsonGenerator.DUMMY_TOKEN_2,
                "refreshToken" to AuthStateJsonGenerator.DUMMY_TOKEN,
                "expiresIn" to 300
            )
        ).toJsonElement()
    )

    private val mockedRefreshGetIdResponse = MockResponse(
        CognitoType.CognitoIdentity,
        "getId",
        ResponseType.Success,
        mapOf("identityId" to "someIdentityId").toJsonElement()
    )

    private val mockedRefreshGetIdFailureResponse = MockResponse(
        CognitoType.CognitoIdentity,
        "getId",
        ResponseType.Failure,
        TooManyRequestsException.invoke {
            message = "Error type: Client, Protocol response: (empty response)"
        }.toJsonElement()
    )

    private val mockedRefreshGetAWSCredentialsResponse = MockResponse(
        CognitoType.CognitoIdentity,
        "getCredentialsForIdentity",
        ResponseType.Success,
        mapOf(
            "credentials" to mapOf(
                "accessKeyId" to "someAccessKey",
                "secretKey" to "someSecretKey",
                "sessionToken" to AuthStateJsonGenerator.DUMMY_TOKEN_2,
                "expiration" to 2342134
            )
        ).toJsonElement()
    )

    private val mockedRefreshGetAWSCredentialsFailureResponse = MockResponse(
        CognitoType.CognitoIdentity,
        "getCredentialsForIdentity",
        ResponseType.Failure,
        TooManyRequestsException.invoke {
            message = "Error type: Client, Protocol response: (empty response)"
        }.toJsonElement()
    )

    private val mockedRefreshGetTokensFromRefreshTokensFailureResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "getTokensFromRefreshTokens",
        ResponseType.Failure,
        ResourceNotFoundException.invoke {
            message = "Error type: Client, Protocol response: (empty response)"
        }.toJsonElement()
    )

    private val expectedSuccess = AWSCognitoAuthSession(
        isSignedIn = true,
        identityIdResult = AuthSessionResult.success("someIdentityId"),
        awsCredentialsResult = AuthSessionResult.success(
            AWSCredentials.createAWSCredentials(
                AuthStateJsonGenerator.ACCESS_KEY_ID,
                AuthStateJsonGenerator.SECRET_ACCESS_KEY,
                AuthStateJsonGenerator.DUMMY_TOKEN,
                AuthStateJsonGenerator.EXPIRATION
            )
        ),
        userSubResult = AuthSessionResult.success(AuthStateJsonGenerator.USER_ID),
        userPoolTokensResult = AuthSessionResult.success(
            AWSCognitoUserPoolTokens(
                accessToken = AuthStateJsonGenerator.DUMMY_TOKEN,
                idToken = AuthStateJsonGenerator.DUMMY_TOKEN,
                refreshToken = AuthStateJsonGenerator.DUMMY_TOKEN
            )
        )
    ).toJsonElement()

    private val expectedRefreshSuccess = AWSCognitoAuthSession(
        isSignedIn = true,
        identityIdResult = AuthSessionResult.success("someIdentityId"),
        awsCredentialsResult = AuthSessionResult.success(
            AWSCredentials.createAWSCredentials(
                AuthStateJsonGenerator.ACCESS_KEY_ID,
                AuthStateJsonGenerator.SECRET_ACCESS_KEY,
                AuthStateJsonGenerator.DUMMY_TOKEN_2,
                AuthStateJsonGenerator.EXPIRATION
            )
        ),
        userSubResult = AuthSessionResult.success(AuthStateJsonGenerator.USER_ID),
        userPoolTokensResult = AuthSessionResult.success(
            AWSCognitoUserPoolTokens(
                accessToken = AuthStateJsonGenerator.DUMMY_TOKEN_2,
                idToken = AuthStateJsonGenerator.DUMMY_TOKEN_2,
                refreshToken = AuthStateJsonGenerator.DUMMY_TOKEN
            )
        )
    ).toJsonElement()

    private val unknownRefreshException = UnknownException(
        message = "Fetch auth session failed.",
        cause = ResourceNotFoundException.invoke { }
    )

    private val identityRefreshException = UnknownException(
        message = "Fetch auth session failed.",
        cause = TooManyRequestsException.invoke { }
    )

    private val expectedRefreshFailure = AWSCognitoAuthSession(
        isSignedIn = true,
        identityIdResult = AuthSessionResult.failure(unknownRefreshException),
        awsCredentialsResult = AuthSessionResult.failure(unknownRefreshException),
        userSubResult = AuthSessionResult.failure(unknownRefreshException),
        userPoolTokensResult = AuthSessionResult.failure(unknownRefreshException)
    ).toJsonElement()

    private val expectedRefreshIdentityFailure = AWSCognitoAuthSession(
        isSignedIn = true,
        identityIdResult = AuthSessionResult.failure(identityRefreshException),
        awsCredentialsResult = AuthSessionResult.failure(identityRefreshException),
        userSubResult = AuthSessionResult.failure(identityRefreshException),
        userPoolTokensResult = AuthSessionResult.failure(identityRefreshException)
    ).toJsonElement()

    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchAuthSession,
        ResponseType.Success,
        expectedSuccess
    )

    private val apiRefreshReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchAuthSession,
        ResponseType.Success,
        expectedRefreshSuccess
    )

    private val apiRefreshFailureReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchAuthSession,
        ResponseType.Success,
        expectedRefreshFailure
    )

    private val apiRefreshIdentityFailureReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchAuthSession,
        ResponseType.Success,
        expectedRefreshIdentityFailure
    )

    private val baseCase = FeatureTestCase(
        description = "AuthSession object is successfully returned for UserAndIdentity Pool",
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

    private val refreshSuccessCase: FeatureTestCase = baseCase.copy(
        description = "AuthSession object is successfully returned after refresh",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(
                mockedRefreshGetTokensFromRefreshTokensResponse,
                mockedRefreshGetIdResponse,
                mockedRefreshGetAWSCredentialsResponse
            )
        ),
        api = API(
            name = AuthAPI.fetchAuthSession,
            params = JsonObject(emptyMap()),
            options = mapOf("forceRefresh" to true).toJsonElement()
        ),
        validations = listOf(apiRefreshReturnValidation)
    )

    private val refreshFailureCase: FeatureTestCase = baseCase.copy(
        description = "AuthSession object is successfully returned after failed refresh",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(mockedRefreshGetTokensFromRefreshTokensFailureResponse)
        ),
        api = API(
            name = AuthAPI.fetchAuthSession,
            params = JsonObject(emptyMap()),
            options = mapOf("forceRefresh" to true).toJsonElement()
        ),
        validations = listOf(apiRefreshFailureReturnValidation)
    )

    private val refreshUserPoolSuccessIdentityPoolFailureCase = baseCase.copy(
        description = "AuthSession object is successfully returned after failed identity refresh",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(
                mockedRefreshGetTokensFromRefreshTokensResponse,
                mockedRefreshGetIdFailureResponse,
                mockedRefreshGetAWSCredentialsFailureResponse
            )
        ),
        api = API(
            name = AuthAPI.fetchAuthSession,
            params = JsonObject(emptyMap()),
            options = mapOf("forceRefresh" to true).toJsonElement()
        ),
        validations = listOf(apiRefreshIdentityFailureReturnValidation)
    )

    private val identityPoolCase: FeatureTestCase = baseCase.copy(
        description = "AuthSession object is successfully returned for Identity Pool",
        preConditions = baseCase.preConditions.copy(
            state = "SignedOut_IdentityPoolConfigured.json"
        ),
        api = baseCase.api,
        validations = listOf(
            ExpectationShapes.Amplify(
                AuthAPI.fetchAuthSession,
                ResponseType.Success,
                AWSCognitoAuthSession(
                    isSignedIn = false,
                    identityIdResult = AuthSessionResult.success("someIdentityId"),
                    awsCredentialsResult = AuthSessionResult.success(
                        AWSCredentials.createAWSCredentials(
                            AuthStateJsonGenerator.ACCESS_KEY_ID,
                            AuthStateJsonGenerator.SECRET_ACCESS_KEY,
                            AuthStateJsonGenerator.DUMMY_TOKEN,
                            AuthStateJsonGenerator.EXPIRATION
                        )
                    ),
                    userSubResult = AuthSessionResult.failure(SignedOutException()),
                    userPoolTokensResult = AuthSessionResult.failure(SignedOutException())
                ).toJsonElement()
            )
        )
    )

    private val userPoolCase: FeatureTestCase = baseCase.copy(
        description = "AuthSession object is successfully returned for User Pool",
        preConditions = baseCase.preConditions.copy(
            state = "SignedIn_UserPoolSessionEstablished.json"
        ),
        api = baseCase.api,
        validations = listOf(
            ExpectationShapes.Amplify(
                AuthAPI.fetchAuthSession,
                ResponseType.Success,
                AWSCognitoAuthSession(
                    isSignedIn = true,
                    identityIdResult = AuthSessionResult.failure(
                        ConfigurationException(
                            "Could not retrieve Identity ID",
                            "Cognito Identity not configured. Please check amplifyconfiguration.json file."
                        )
                    ),
                    awsCredentialsResult = AuthSessionResult.failure(
                        ConfigurationException(
                            "Could not fetch AWS Cognito credentials",
                            "Cognito Identity not configured. Please check amplifyconfiguration.json file."
                        )
                    ),
                    userSubResult = AuthSessionResult.success(AuthStateJsonGenerator.USER_ID),
                    userPoolTokensResult = AuthSessionResult.success(
                        AWSCognitoUserPoolTokens(
                            accessToken = AuthStateJsonGenerator.DUMMY_TOKEN,
                            idToken = AuthStateJsonGenerator.DUMMY_TOKEN,
                            refreshToken = AuthStateJsonGenerator.DUMMY_TOKEN
                        )
                    )
                ).toJsonElement()
            )
        )
    )

    override val serializables: List<Any> = listOf(
        baseCase,
        refreshSuccessCase,
        refreshFailureCase,
        refreshUserPoolSuccessIdentityPoolFailureCase,
        identityPoolCase,
        userPoolCase
    )
}
