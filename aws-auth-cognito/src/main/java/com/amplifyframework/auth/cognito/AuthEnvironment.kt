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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.events.SignUpEvent

class AuthEnvironment internal constructor(
    val configuration: AuthConfiguration,
    val cognitoAuthService: AWSCognitoAuthServiceBehavior,
    val logger: Logger? = null
) : Environment {
    internal lateinit var srpHelper: SRPHelper
}

fun StateMachineEvent.isAuthEvent(): AuthEvent.EventType? {
    return (this as? AuthEvent)?.eventType
}

fun StateMachineEvent.isAuthenticationEvent(): AuthenticationEvent.EventType? {
    return (this as? AuthenticationEvent)?.eventType
}

fun StateMachineEvent.isAuthorizationEvent(): AuthorizationEvent.EventType? {
    return (this as? AuthorizationEvent)?.eventType
}

fun StateMachineEvent.isSignOutEvent(): SignOutEvent.EventType? {
    return (this as? SignOutEvent)?.eventType
}

fun StateMachineEvent.isSignUpEvent(): SignUpEvent.EventType? {
    return (this as? SignUpEvent)?.eventType
}

fun StateMachineEvent.isDeleteUserEvent(): DeleteUserEvent.EventType? {
    return (this as? DeleteUserEvent)?.eventType
}
