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

import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.FetchAuthSessionActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.FetchAwsCredentialsEvent
import com.amplifyframework.statemachine.codegen.events.FetchIdentityEvent
import com.amplifyframework.statemachine.codegen.events.FetchUserPoolTokensEvent

object FetchAuthSessionCognitoActions : FetchAuthSessionActions {
    override fun configureUserPoolTokensAction(amplifyCredential: AmplifyCredential?): Action =
        Action<AuthEnvironment>("ConfigureUserPoolTokens") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val userPoolTokens = amplifyCredential?.cognitoUserPoolTokens
            val evt = if (userPoolTokens != null) {
                if (SessionHelper.isValid(userPoolTokens)) {
                    // User Pool Tokens (id Token and access Token) are valid
                    val fetchedEvent = FetchUserPoolTokensEvent(FetchUserPoolTokensEvent.EventType.Fetched())
                    logger?.verbose("$id Sending event ${fetchedEvent.type}")
                    dispatcher.send(fetchedEvent)

                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
                } else {
                    // Tokens have expired and we need a refresh
                    Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SESSION_EXPIRED))
                    FetchUserPoolTokensEvent(FetchUserPoolTokensEvent.EventType.Refresh(amplifyCredential))
                }
            } else {
                // Failure to fetch token, fetch guest identity
                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun configureIdentityAction(amplifyCredential: AmplifyCredential?): Action =
        Action<AuthEnvironment>("ConfigureIdentity") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val identityID = amplifyCredential?.identityId
            val evt = if (identityID != null) {
                val fetchedEvent = FetchIdentityEvent(FetchIdentityEvent.EventType.Fetched())
                logger?.verbose("$id Sending event ${fetchedEvent.type}")
                dispatcher.send(fetchedEvent)

                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchAwsCredentials(amplifyCredential))
            } else {
                FetchIdentityEvent(FetchIdentityEvent.EventType.Fetch(amplifyCredential))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun configureAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action =
        Action<AuthEnvironment>("ConfigureAWSCredentials") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val awsCredentials = amplifyCredential?.awsCredentials
            val evt = if (awsCredentials != null) {
                // TODO: fix expiry
                val fetchedEvent = FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.Fetched())
                logger?.verbose("$id Sending event ${fetchedEvent.type}")
                dispatcher.send(fetchedEvent)

                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(amplifyCredential))
            } else {
                FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.Fetch(amplifyCredential))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun authorizationSessionEstablished(amplifyCredential: AmplifyCredential?): Action =
        Action<AuthEnvironment>("AuthZSessionEstablished") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = AuthorizationEvent(AuthorizationEvent.EventType.Fetched(amplifyCredential))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
