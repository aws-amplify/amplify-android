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

import android.net.Uri
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.SignInData
import java.lang.Exception
import java.util.Date

internal class HostedUIEvent(val eventType: EventType, override val time: Date? = null) : StateMachineEvent {

    sealed class EventType {
        data class ShowHostedUI(val hostedUISignInData: SignInData.HostedUISignInData) : EventType()
        data class FetchToken(val uri: Uri) : EventType()
        object TokenFetched : EventType()
        data class ThrowError(val exception: Exception) : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
