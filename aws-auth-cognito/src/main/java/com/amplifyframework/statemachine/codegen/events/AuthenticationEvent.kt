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

package com.amplifyframework.statemachine.codegen.events

import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import java.util.Date

internal class AuthenticationEvent(val eventType: EventType, override val time: Date? = null) :
    StateMachineEvent {
    sealed class EventType {
        data class Configure(
            val configuration: AuthConfiguration,
            val storedCredentials: AmplifyCredential
        ) : EventType()

        object Configured : EventType()
        data class InitializedSignedIn(val signedInData: SignedInData, val deviceMetadata: DeviceMetadata) : EventType()
        data class InitializedSignedOut(val signedOutData: SignedOutData) : EventType()
        object InitializedFederated : EventType()
        data class SignInRequested(val signInData: SignInData) : EventType()
        data class SignInCompleted(val signedInData: SignedInData, val deviceMetadata: DeviceMetadata) : EventType()
        data class SignOutRequested(val signOutData: SignOutData) : EventType()
        data class CancelSignIn(val error: Exception? = null) : EventType()
        data class CancelSignOut(val signedInData: SignedInData, val deviceMetadata: DeviceMetadata) : EventType()
        data class ClearFederationToIdentityPool(val id: String = "") : EventType()
        data class ThrowError(val exception: Exception) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
