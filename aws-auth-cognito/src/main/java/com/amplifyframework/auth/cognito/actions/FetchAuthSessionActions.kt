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
import java.time.ZonedDateTime

object FetchAuthSessionActions : FetchAuthSessionActions {
    override fun configureUserPoolTokensAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, _ ->
            val userPoolTokens = amplifyCredential?.cognitoUserPoolTokens
            if (userPoolTokens == null) {
                // Failure to fetch, Refresh
                val event = FetchUserPoolTokensEvent(
                    FetchUserPoolTokensEvent.EventType.Refresh(amplifyCredential)
                )
                dispatcher.send(event)
            }
            val userPoolTokenExpiryTime = userPoolTokens?.tokenExpiration
            val rightNow = ZonedDateTime.now()

            // The token must be valid for 2 minutes from now
            if (userPoolTokenExpiryTime != null) {
                if (userPoolTokenExpiryTime > (rightNow.toEpochSecond() + (2 * 60))) {
                    // User Pool Token is still valid
                    val event =
                        FetchUserPoolTokensEvent(
                            FetchUserPoolTokensEvent.EventType.Fetched(amplifyCredential)
                        )
                    dispatcher.send(event)
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchIdentity(
                                amplifyCredential
                            )
                        )
                    )
                } else {
                    // Token has expired and we need a refresh
                    val event = FetchUserPoolTokensEvent(
                        FetchUserPoolTokensEvent.EventType.Refresh(amplifyCredential)
                    )
                    dispatcher.send(event)
                    Amplify.Hub.publish(
                        HubChannel.AUTH,
                        HubEvent.create(AuthChannelEventName.SESSION_EXPIRED)
                    )
                }
            }
        }

    override fun configureIdentityAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, _ ->
            val identityID = amplifyCredential?.identityId
            if (identityID != null) {
                val event = FetchIdentityEvent(
                    FetchIdentityEvent.EventType.Fetched(amplifyCredential)
                )
                dispatcher.send(event)
                dispatcher.send(
                    FetchAuthSessionEvent(
                        FetchAuthSessionEvent.EventType.FetchAwsCredentials(
                            amplifyCredential
                        )
                    )
                )
            } else {
                val event = FetchIdentityEvent(
                    FetchIdentityEvent.EventType.Fetch(amplifyCredential)
                )
                dispatcher.send(event)
            }
        }

    override fun configureAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action =
        Action { dispatcher, _ ->
            val awsCredentials = amplifyCredential?.awsCredentials
            // AWS Credentials should be valid for up to 2 minutes from now
            if (awsCredentials?.expiration != null && awsCredentials.expiration > (
                ZonedDateTime.now()
                    .toEpochSecond() + 2 * 60
                )
            ) {
                val event =
                    FetchAwsCredentialsEvent(
                        FetchAwsCredentialsEvent.EventType.Fetched(amplifyCredential)
                    )
                dispatcher.send(event)
                dispatcher.send(
                    FetchAuthSessionEvent(
                        FetchAuthSessionEvent.EventType.FetchedAuthSession(
                            amplifyCredential
                        )
                    )
                )
                dispatcher.send(
                    AuthorizationEvent(
                        AuthorizationEvent.EventType.FetchedAuthSession(
                            amplifyCredential
                        )
                    )
                )
            } else {
                val event = FetchAwsCredentialsEvent(
                    FetchAwsCredentialsEvent.EventType.Fetch(
                        amplifyCredential
                    )
                )
                dispatcher.send(event)
                Amplify.Hub.publish(
                    HubChannel.AUTH,
                    HubEvent.create(AuthChannelEventName.SESSION_EXPIRED)
                )
            }
        }
}
