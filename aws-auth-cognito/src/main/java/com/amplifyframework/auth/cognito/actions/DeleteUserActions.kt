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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeleteUserRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.DeleteUserActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent

object DeleteUserActions : DeleteUserActions {
    override fun initDeleteUserAction(accessToken: String): Action =
        Action<AuthEnvironment>("DeleteUser") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                cognitoAuthService.cognitoIdentityProviderClient?.deleteUser(
                    DeleteUserRequest.invoke { accessToken }
                )
                DeleteUserEvent(DeleteUserEvent.EventType.SignOutDeletedUser())
            } catch (e: Exception) {
                logger?.warn("Failed to delete user.", e)
                DeleteUserEvent(DeleteUserEvent.EventType.ThrowError(e))
            }
            logger?.verbose("$id Sending event $evt")
            dispatcher.send(evt)
        }
}
