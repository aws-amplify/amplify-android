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

import android.app.Activity
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import java.lang.ref.WeakReference
import java.util.Date

internal class SignInEvent(val eventType: EventType, override val time: Date? = null) : StateMachineEvent {
    sealed class EventType {
        data class InitiateSignInWithSRP(
            val username: String,
            val password: String,
            val metadata: Map<String, String>,
            val authFlowType: AuthFlowType,
            val respondToAuthChallenge: AuthChallenge? = null
        ) : EventType()

        data class InitiateSignInWithCustom(val username: String, val metadata: Map<String, String>) : EventType()

        data class InitiateCustomSignInWithSRP(
            val username: String,
            val password: String,
            val metadata: Map<String, String>
        ) : EventType()

        data class InitiateMigrateAuth(
            val username: String,
            val password: String,
            val metadata: Map<String, String>,
            val authFlowType: AuthFlowType,
            val respondToAuthChallenge: AuthChallenge? = null
        ) : EventType()

        data class InitiateHostedUISignIn(val hostedUISignInData: SignInData.HostedUISignInData) : EventType()
        data class SignedIn(val id: String = "") : EventType()
        data class InitiateSignInWithDeviceSRP(val username: String, val metadata: Map<String, String>) : EventType()

        data class ConfirmDevice(val deviceMetadata: DeviceMetadata.Metadata, val signedInData: SignedInData) :
            EventType()
        data class FinalizeSignIn(val id: String = "") : EventType()
        data class ReceivedChallenge(val challenge: AuthChallenge, val signInMethod: SignInMethod) : EventType()
        data class ThrowError(val exception: Exception) : EventType()
        data class InitiateTOTPSetup(
            val signInTOTPSetupData: SignInTOTPSetupData,
            val challengeParams: Map<String, String>?,
            val signInMethod: SignInMethod
        ) : EventType()

        data class InitiateUserAuth(
            val username: String,
            val preferredChallenge: AuthFactorType?,
            val callingActivity: WeakReference<Activity>,
            val metadata: Map<String, String>
        ) : EventType()
        data class InitiateWebAuthnSignIn(val signInContext: WebAuthnSignInContext) : EventType()
        data class InitiateAutoSignIn(val signInData: SignInData.AutoSignInData) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
