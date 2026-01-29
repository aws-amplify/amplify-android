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

package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.listDevices
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.requireSignedInState

internal class FetchDevicesUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine
) {
    suspend fun execute(): List<AuthDevice> {
        stateMachine.requireSignedInState()
        val token = fetchAuthSession.execute().accessToken
        val response = client.listDevices { accessToken = token }
        return response.devices?.map { device ->
            val id = device.deviceKey ?: ""
            val name = device.deviceAttributes?.find { it.name == "device_name" }?.value
            AuthDevice.fromId(id, name)
        } ?: emptyList()
    }
}
