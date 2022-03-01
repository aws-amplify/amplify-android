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

package com.amplifyframework.auth.cognito.events

import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.data.AuthConfiguration
import com.amplifyframework.auth.cognito.data.AuthorizationError
import com.amplifyframework.statemachine.StateMachineEvent
import java.util.*

class AuthorizationEvent(val eventType: EventType, override val time: Date? = null) :
        StateMachineEvent {
    sealed class EventType {
        data class Configure(val configuration: AuthConfiguration) : EventType()
        data class FetchAuthSession(val credentials: AmplifyCredential) : EventType()
        // TODO change session to AWSAuthCognitoSession type
        data class FetchedAuthSession(val session: String) : EventType()
        data class ThrowError(val error: AuthorizationError) : EventType()
    }

    override val type = eventType.toString()
}