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

package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.initiateAuth
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidOauthConfigurationException
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchAuthSessionActions
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.RefreshSessionEvent
import kotlin.time.Duration.Companion.seconds

internal object FetchAuthSessionCognitoActions : FetchAuthSessionActions {
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_REFRESH_TOKEN = "REFRESH_TOKEN"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"

    override fun refreshUserPoolTokensAction(signedInData: SignedInData) =
        Action<AuthEnvironment>("RefreshUserPoolTokens") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val username = signedInData.username
                val tokens = signedInData.cognitoUserPoolTokens

                val authParameters = mutableMapOf<String, String>()
                val secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                tokens.refreshToken?.let { authParameters[KEY_REFRESH_TOKEN] = it }
                secretHash?.let { authParameters[KEY_SECRET_HASH] = it }

                val encodedContextData = getUserContextData(username)
                val deviceMetadata: DeviceMetadata.Metadata? = getDeviceMetadata(username)
                deviceMetadata?.let { authParameters[KEY_DEVICE_KEY] = it.deviceKey }
                val pinpointEndpointId = getPinpointEndpointId()

                val response = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.RefreshToken
                    clientId = configuration.userPool?.appClient
                    this.authParameters = authParameters
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { userContextData { encodedData = it } }
                }

                val expiresIn = response?.authenticationResult?.expiresIn?.toLong() ?: 0
                val refreshedUserPoolTokens = CognitoUserPoolTokens(
                    idToken = response?.authenticationResult?.idToken,
                    accessToken = response?.authenticationResult?.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiration = Instant.now().plus(expiresIn.seconds).epochSeconds
                )

                val updatedSignedInData = signedInData.copy(
                    userId = refreshedUserPoolTokens.accessToken?.let(SessionHelper::getUserSub) ?: signedInData.userId,
                    username = refreshedUserPoolTokens.accessToken?.let(SessionHelper::getUsername) ?: username,
                    cognitoUserPoolTokens = refreshedUserPoolTokens
                )

                if (configuration.identityPool != null) {
                    val logins = LoginsMapProvider.CognitoUserPoolLogins(
                        configuration.userPool?.region,
                        configuration.userPool?.poolId,
                        refreshedUserPoolTokens.idToken!!
                    )
                    RefreshSessionEvent(RefreshSessionEvent.EventType.RefreshAuthSession(updatedSignedInData, logins))
                } else {
                    RefreshSessionEvent(RefreshSessionEvent.EventType.Refreshed(updatedSignedInData))
                }
            } catch (notAuthorized: aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException) {
                val error = SessionExpiredException(cause = notAuthorized)
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(error))
            } catch (e: Exception) {
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(e))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun refreshHostedUIUserPoolTokensAction(signedInData: SignedInData) =
        Action<AuthEnvironment>("RefreshHostedUITokens") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val username = signedInData.username
                val refreshToken = signedInData.cognitoUserPoolTokens.refreshToken
                if (hostedUIClient == null) throw InvalidOauthConfigurationException()
                if (refreshToken == null) throw UnknownException("Unable to refresh token due to missing refreshToken.")

                val refreshedUserPoolTokens = hostedUIClient.fetchRefreshedToken(
                    signedInData.cognitoUserPoolTokens.refreshToken
                ).copy(
                    // A refresh does not provide a new refresh token,
                    // so we rebuild the new token with the old refresh token.
                    refreshToken = signedInData.cognitoUserPoolTokens.refreshToken
                )

                val updatedSignedInData = signedInData.copy(
                    userId = refreshedUserPoolTokens.accessToken?.let(SessionHelper::getUserSub) ?: signedInData.userId,
                    username = refreshedUserPoolTokens.accessToken?.let(SessionHelper::getUsername) ?: username,
                    cognitoUserPoolTokens = refreshedUserPoolTokens
                )

                if (configuration.identityPool != null) {
                    val logins = LoginsMapProvider.CognitoUserPoolLogins(
                        configuration.userPool?.region,
                        configuration.userPool?.poolId,
                        refreshedUserPoolTokens.idToken!!
                    )
                    RefreshSessionEvent(RefreshSessionEvent.EventType.RefreshAuthSession(updatedSignedInData, logins))
                } else {
                    RefreshSessionEvent(RefreshSessionEvent.EventType.Refreshed(updatedSignedInData))
                }
            } catch (notAuthorized: aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException) {
                // TODO: identity not authorized exception from response
                val error = SessionExpiredException(cause = notAuthorized)
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(error))
            } catch (e: Exception) {
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(e))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun refreshAuthSessionAction(logins: LoginsMapProvider) =
        Action<AuthEnvironment>("RefreshAuthSession") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(logins))
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun fetchIdentityAction(loginsMap: LoginsMapProvider) =
        Action<AuthEnvironment>("FetchIdentity") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val request = GetIdRequest {
                    identityPoolId = configuration.identityPool?.poolId
                    this.logins = loginsMap.logins
                }

                val response = cognitoAuthService.cognitoIdentityClient?.getId(request)

                response?.identityId?.let {
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchAwsCredentials(it, loginsMap))
                } ?: throw Exception("Fetching identity id failed.")
            } catch (notAuthorized: aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException) {
                val exception = NotAuthorizedException(
                    recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED,
                    cause = notAuthorized
                )
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(exception))
            } catch (e: Exception) {
                val exception = SignedOutException(
                    recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE,
                    cause = e
                )
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(exception))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun fetchAWSCredentialsAction(identityId: String, loginsMap: LoginsMapProvider) =
        Action<AuthEnvironment>("FetchAWSCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val request = GetCredentialsForIdentityRequest {
                    this.identityId = identityId
                    this.logins = loginsMap.logins
                }

                val response = cognitoAuthService.cognitoIdentityClient?.getCredentialsForIdentity(request)

                response?.credentials?.let {
                    val credentials = AWSCredentials(
                        it.accessKeyId,
                        it.secretKey,
                        it.sessionToken,
                        it.expiration?.epochSeconds
                    )
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.Fetched(identityId, credentials))
                } ?: throw Exception("Fetching AWS credentials failed.")
            } catch (notAuthorized: aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException) {
                val exception = NotAuthorizedException(
                    recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED,
                    cause = notAuthorized
                )
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(exception))
            } catch (e: Exception) {
                val exception = SignedOutException(
                    recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE,
                    cause = e
                )
                AuthorizationEvent(AuthorizationEvent.EventType.ThrowError(exception))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun notifySessionEstablishedAction(identityId: String, awsCredentials: AWSCredentials) =
        Action<AuthEnvironment>("NotifySessionEstablished") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = AuthorizationEvent(AuthorizationEvent.EventType.Fetched(identityId, awsCredentials))
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun notifySessionRefreshedAction(amplifyCredential: AmplifyCredential) =
        Action<AuthEnvironment>("NotifySessionRefreshed") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = AuthorizationEvent(AuthorizationEvent.EventType.Refreshed(amplifyCredential))
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
