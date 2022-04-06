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
        Action { dispatcher, _ ->
            val userPoolTokens = amplifyCredential?.cognitoUserPoolTokens
            if (userPoolTokens != null) {
                if (SessionHelper.isValid(userPoolTokens)) {
                    // User Pool Tokens (id Token and access Token) are valid
                    dispatcher.send(FetchUserPoolTokensEvent(FetchUserPoolTokensEvent.EventType.Fetched()))
                    dispatcher.send(
                        FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
                    )
                } else {
                    // Tokens have expired and we need a refresh
                    dispatcher.send(
                        FetchUserPoolTokensEvent(FetchUserPoolTokensEvent.EventType.Refresh(amplifyCredential))
                    )
                    Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SESSION_EXPIRED))
                }
            } else {
                // Failure to fetch token, fetch guest identity
                dispatcher.send(FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential)))
            }
        }

    override fun configureIdentityAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, _ ->
            val identityID = amplifyCredential?.identityId
            if (identityID != null) {
                dispatcher.send(FetchIdentityEvent(FetchIdentityEvent.EventType.Fetched()))
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchAwsCredentials(amplifyCredential))
                )
            } else {
                dispatcher.send(FetchIdentityEvent(FetchIdentityEvent.EventType.Fetch(amplifyCredential)))
            }
        }

    override fun configureAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, _ ->
            val awsCredentials = amplifyCredential?.awsCredentials
            if (awsCredentials != null) {
                // TODO: fix expiry
                dispatcher.send(FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.Fetched()))
                dispatcher.send(
                    FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchedAuthSession(amplifyCredential))
                )
            } else {
                dispatcher.send(FetchAwsCredentialsEvent(FetchAwsCredentialsEvent.EventType.Fetch(amplifyCredential)))
            }
        }

    override fun authorizationSessionEstablished(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, _ ->
            dispatcher.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchedAuthSession(amplifyCredential)))
        }
}
