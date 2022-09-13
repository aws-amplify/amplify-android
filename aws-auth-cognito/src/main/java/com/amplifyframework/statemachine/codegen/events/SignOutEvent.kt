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
import com.amplifyframework.statemachine.codegen.data.GlobalSignOutErrorData
import com.amplifyframework.statemachine.codegen.data.HostedUIErrorData
import com.amplifyframework.statemachine.codegen.data.RevokeTokenErrorData
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import java.util.Date

class SignOutEvent(
    val eventType: EventType,
    override val time: Date? = null,
) : StateMachineEvent {
    sealed class EventType {
        data class InvokeHostedUISignOut(val signOutData: SignOutData, val signedInData: SignedInData) : EventType()

        data class SignOutLocally(
            val signedInData: SignedInData?,
            val hostedUIErrorData: HostedUIErrorData? = null,
            val globalSignOutErrorData: GlobalSignOutErrorData? = null,
            val revokeTokenErrorData: RevokeTokenErrorData? = null
        ) : EventType()

        data class SignOutGlobally(
            val signedInData: SignedInData,
            val hostedUIErrorData: HostedUIErrorData? = null
        ) : EventType()

        data class RevokeToken(
            val signedInData: SignedInData,
            val hostedUIErrorData: HostedUIErrorData? = null,
            val globalSignOutErrorData: GlobalSignOutErrorData? = null
        ) : EventType()

        data class SignOutGloballyError(
            val signedInData: SignedInData,
            val hostedUIErrorData: HostedUIErrorData? = null,
            val globalSignOutErrorData: GlobalSignOutErrorData? = null
        ) : EventType()

        data class SignedOutSuccess(val signedOutData: SignedOutData) : EventType()

        data class UserCancelled(val signedInData: SignedInData) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
