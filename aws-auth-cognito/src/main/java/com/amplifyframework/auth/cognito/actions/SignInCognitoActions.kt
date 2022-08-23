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
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object SignInCognitoActions : SignInActions {
    override fun startSRPAuthAction(event: SignInEvent.EventType.InitiateSignInWithSRP) =
        Action<AuthEnvironment>("StartSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = SRPEvent(SRPEvent.EventType.InitiateSRP(event.username, event.password))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initResolveChallenge(event: SignInEvent.EventType.ReceivedChallenge) =
        Action<AuthEnvironment>("InitResolveChallenge") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = SignInChallengeEvent(SignInChallengeEvent.EventType.WaitForAnswer(event.challenge))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startHostedUIAuthAction(event: SignInEvent.EventType.InitiateHostedUISignIn) =
        Action<AuthEnvironment>("StartHostedUIAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = HostedUIEvent(HostedUIEvent.EventType.ShowHostedUI(event.hostedUISignInData))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
