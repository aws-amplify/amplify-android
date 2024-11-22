/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.cognito.helpers.WebAuthnHelper
import com.amplifyframework.auth.cognito.requireIdentityProviderClient
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.actions.WebAuthnSignInActions
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.data.requireRequestJson
import com.amplifyframework.statemachine.codegen.data.requireResponseJson
import com.amplifyframework.statemachine.codegen.events.WebAuthnEvent

internal object WebAuthnSignInCognitoActions : WebAuthnSignInActions {
    private enum class ChallengeResponse(val key: String) {
        USERNAME("USERNAME"),
        CREDENTIAL("CREDENTIAL"),
        ANSWER("ANSWER")
    }

    override fun fetchCredentialOptions(event: WebAuthnEvent.EventType.FetchCredentialOptions): Action =
        safeAction(event.signInContext) {
            val signInContext = event.signInContext
            val client = requireIdentityProviderClient()
            val encodedContextData = getUserContextData(signInContext.username)
            val pinpointEndpointId = getPinpointEndpointId()

            val response = client.respondToAuthChallenge {
                challengeName = ChallengeNameType.SelectChallenge
                clientId = configuration.userPool?.appClient
                challengeResponses = mapOf(
                    ChallengeResponse.USERNAME.key to signInContext.username,
                    ChallengeResponse.ANSWER.key to ChallengeNameType.WebAuthn.value
                )
                session = signInContext.session
                pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                encodedContextData?.let { userContextData { encodedData = it } }
            }

            SignInChallengeHelper.evaluateNextStep(
                username = signInContext.username,
                challengeNameType = response.challengeName,
                session = response.session,
                challengeParameters = response.challengeParameters,
                authenticationResult = response.authenticationResult,
                callingActivity = signInContext.callingActivity
            )
        }

    override fun assertCredentials(event: WebAuthnEvent.EventType.AssertCredentialOptions): Action =
        safeAction(event.signInContext) {
            val helper = WebAuthnHelper(context)
            val responseJson = helper.getCredential(
                requestJson = event.signInContext.requireRequestJson(),
                callingActivity = event.signInContext.callingActivity
            )
            val newContext = event.signInContext.copy(responseJson = responseJson)
            WebAuthnEvent(WebAuthnEvent.EventType.VerifyCredentialsAndSignIn(newContext))
        }

    override fun verifyCredentialAndSignIn(event: WebAuthnEvent.EventType.VerifyCredentialsAndSignIn): Action =
        safeAction(event.signInContext) {
            val signInContext = event.signInContext
            val client = requireIdentityProviderClient()
            val encodedContextData = getUserContextData(signInContext.username)
            val pinpointEndpointId = getPinpointEndpointId()

            val response = client.respondToAuthChallenge {
                challengeName = ChallengeNameType.WebAuthn
                clientId = configuration.userPool?.appClient
                challengeResponses = mapOf(
                    ChallengeResponse.USERNAME.key to signInContext.username,
                    ChallengeResponse.CREDENTIAL.key to signInContext.requireResponseJson()
                )
                session = signInContext.session
                pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                encodedContextData?.let { userContextData { encodedData = it } }
            }

            SignInChallengeHelper.evaluateNextStep(
                username = signInContext.username,
                challengeNameType = ChallengeNameType.WebAuthn,
                session = signInContext.session,
                challengeParameters = response.challengeParameters,
                authenticationResult = response.authenticationResult,
                callingActivity = signInContext.callingActivity,
                signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
            )
        }

    private fun safeAction(context: WebAuthnSignInContext, block: suspend AuthEnvironment.() -> StateMachineEvent) =
        Action<AuthEnvironment> { _, dispatcher ->
            val evt = try {
                block()
            } catch (e: Exception) {
                WebAuthnEvent(WebAuthnEvent.EventType.ThrowError(e, context))
            }
            dispatcher.send(evt)
        }
}
