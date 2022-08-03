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
import java.util.Date

class AuthEvent(val eventType: EventType, override val time: Date? = null) :
    StateMachineEvent {
    sealed class EventType {
        data class ConfigureAuth(val configuration: AuthConfiguration) : EventType()
        data class FetchCachedCredentials(val configuration: AuthConfiguration) : EventType()
        data class ReceivedCachedCredentials(val storedCredentials: AmplifyCredential?) : EventType()
        object CachedCredentialsFailed : EventType()
        data class ConfigureAuthentication(
            val configuration: AuthConfiguration,
            val storedCredentials: AmplifyCredential?
        ) : EventType()

        data class ConfigureAuthorization(
            val configuration: AuthConfiguration,
            val storedCredentials: AmplifyCredential?
        ) : EventType()

        data class ConfiguredAuthentication(
            val configuration: AuthConfiguration,
            val storedCredentials: AmplifyCredential?
        ) : EventType()

        object ConfiguredAuthorization : EventType()
    }

    override val type: String = eventType.javaClass.simpleName
}
