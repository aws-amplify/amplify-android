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

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent

object AuthorizationCognitoActions : AuthorizationActions {
    override fun resetAuthorizationAction() = Action<AuthEnvironment>("resetAuthZ") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        // TODO: recover from error
//        val evt = AuthorizationEvent(AuthorizationEvent.EventType.Configure(configuration))
//        logger?.verbose("$id Sending event ${evt.type}")
//        dispatcher.send(evt)
    }

    override fun configureAuthorizationAction() = Action<AuthEnvironment>("ConfigureAuthZ") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        val evt = AuthEvent(AuthEvent.EventType.ConfiguredAuthorization)
        logger?.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    override fun initializeFetchAuthSession(amplifyCredential: AmplifyCredential?) =
        Action<AuthEnvironment>("InitFetchAuthSession") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = amplifyCredential?.cognitoUserPoolTokens?.let {
                FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchUserPoolTokens(amplifyCredential))
            } ?: FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(amplifyCredential))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
