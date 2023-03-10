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

import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.RefreshSessionEvent

internal object AuthorizationCognitoActions : AuthorizationActions {
    override fun configureAuthorizationAction() = Action<AuthEnvironment>("ConfigureAuthZ") { id, dispatcher ->
        logger.verbose("$id Starting execution")
        val evt = AuthEvent(AuthEvent.EventType.ConfiguredAuthorization)
        logger.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    override fun initializeFetchUnAuthSession() =
        Action<AuthEnvironment>("InitFetchUnAuthSession") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = configuration.identityPool?.poolId?.let {
                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(LoginsMapProvider.UnAuthLogins()))
            } ?: AuthorizationEvent(
                AuthorizationEvent.EventType.ThrowError(
                    ConfigurationException(
                        "Identity Pool not configured.",
                        "Please check amplifyconfiguration.json file."
                    )
                )
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initializeFetchAuthSession(signedInData: SignedInData) =
        Action<AuthEnvironment>("InitFetchAuthSession") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = when {
                configuration.userPool?.poolId == null -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(
                        ConfigurationException(
                            "User Pool not configured.",
                            "Please check amplifyconfiguration.json file."
                        )
                    )
                )
                configuration.identityPool?.poolId == null -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(
                        ConfigurationException(
                            "Identity Pool not configured.",
                            "Please check amplifyconfiguration.json file."
                        )
                    )
                )
                signedInData.cognitoUserPoolTokens.idToken == null -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(
                        ConfigurationException(
                            "Identity token is null.",
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        )
                    )
                )
                else -> {
                    val logins = LoginsMapProvider.CognitoUserPoolLogins(
                        configuration.userPool.region,
                        configuration.userPool.poolId,
                        signedInData.cognitoUserPoolTokens.idToken
                    )
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(logins))
                }
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initiateRefreshSessionAction(amplifyCredential: AmplifyCredential) =
        Action<AuthEnvironment>("InitiateRefreshSession") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = when (amplifyCredential) {
                is AmplifyCredential.UserPoolTypeCredential -> RefreshSessionEvent(
                    RefreshSessionEvent.EventType.RefreshUserPoolTokens(amplifyCredential.signedInData)
                )
                is AmplifyCredential.IdentityPool -> RefreshSessionEvent(
                    RefreshSessionEvent.EventType.RefreshUnAuthSession(LoginsMapProvider.UnAuthLogins())
                )
                is AmplifyCredential.IdentityPoolFederated -> {
                    AuthorizationEvent(
                        AuthorizationEvent.EventType.ThrowError(
                            Exception("Refreshing credentials from federationToIdentityPool is not supported.")
                        )
                    )
                }
                else -> AuthorizationEvent(
                    AuthorizationEvent.EventType.ThrowError(Exception("Credentials empty, cannot refresh."))
                )
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initializeFederationToIdentityPool(
        federatedToken: FederatedToken,
        developerProvidedIdentityId: String?
    ) = Action<AuthEnvironment>("InitializeFederationToIdentityPool") { id, dispatcher ->
        logger.verbose("$id Starting execution")
        val logins = LoginsMapProvider.AuthProviderLogins(federatedToken)
        val evt = if (developerProvidedIdentityId != null) {
            FetchAuthSessionEvent(
                FetchAuthSessionEvent.EventType.FetchAwsCredentials(developerProvidedIdentityId, logins)
            )
        } else {
            FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(logins))
        }
        logger.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    override fun initiateDeleteUser(event: DeleteUserEvent.EventType.DeleteUser) =
        Action<AuthEnvironment>("InitiateDeleteUser") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = DeleteUserEvent(DeleteUserEvent.EventType.DeleteUser(event.accessToken))
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun persistCredentials(amplifyCredential: AmplifyCredential) =
        Action<AuthEnvironment>("PersistCredentials") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                credentialStoreClient.storeCredentials(CredentialType.Amplify, amplifyCredential)
                AuthEvent(AuthEvent.EventType.ReceivedCachedCredentials(amplifyCredential))
            } catch (e: Exception) {
                AuthEvent(AuthEvent.EventType.CachedCredentialsFailed)
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
