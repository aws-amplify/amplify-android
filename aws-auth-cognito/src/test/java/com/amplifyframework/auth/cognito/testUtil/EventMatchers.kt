/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.testUtil

import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.events.WebAuthnEvent
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.MockKAssertScope
import io.mockk.MockKVerificationScope

internal inline fun <reified T : WebAuthnEvent.EventType> MockKVerificationScope.withWebAuthnEvent(
    noinline assertions: MockKAssertScope.(T) -> Unit = { }
) = withArg<StateMachineEvent> {
    val event = it.shouldBeInstanceOf<WebAuthnEvent>()
    val type = event.eventType.shouldBeInstanceOf<T>()
    assertions(type)
}

internal inline fun <reified T : SignInEvent.EventType> MockKVerificationScope.withSignInEvent(
    noinline assertions: MockKAssertScope.(T) -> Unit = { }
) = withArg<StateMachineEvent> {
    val event = it.shouldBeInstanceOf<SignInEvent>()
    val type = event.eventType.shouldBeInstanceOf<T>()
    assertions(type)
}

internal inline fun <reified T : SignUpEvent.EventType> MockKVerificationScope.withSignUpEvent(
    noinline assertions: MockKAssertScope.(T) -> Unit = { }
) = withArg<StateMachineEvent> {
    val event = it.shouldBeInstanceOf<SignUpEvent>()
    val type = event.eventType.shouldBeInstanceOf<T>()
    assertions(type)
}

internal inline fun <reified T : AuthenticationEvent.EventType> MockKVerificationScope.withAuthEvent(
    noinline assertions: MockKAssertScope.(T) -> Unit = { }
) = withArg<StateMachineEvent> {
    val event = it.shouldBeInstanceOf<AuthenticationEvent>()
    val type = event.eventType.shouldBeInstanceOf<T>()
    assertions(type)
}

internal inline fun <reified T : AuthorizationEvent.EventType> MockKVerificationScope.withAuthZEvent(
    noinline assertions: MockKAssertScope.(T) -> Unit = { }
) = withArg<StateMachineEvent> {
    val event = it.shouldBeInstanceOf<AuthorizationEvent>()
    val type = event.eventType.shouldBeInstanceOf<T>()
    assertions(type)
}

internal inline fun <reified T : DeleteUserEvent.EventType> MockKVerificationScope.withDeleteEvent(
    noinline assertions: MockKAssertScope.(T) -> Unit = { }
) = withArg<StateMachineEvent> {
    val event = it.shouldBeInstanceOf<DeleteUserEvent>()
    val type = event.eventType.shouldBeInstanceOf<T>()
    assertions(type)
}
