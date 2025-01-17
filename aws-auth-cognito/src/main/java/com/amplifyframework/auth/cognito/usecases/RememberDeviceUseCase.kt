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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceRememberedStatusType
import aws.sdk.kotlin.services.cognitoidentityprovider.updateDeviceStatus
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.requireSignedInState

internal class RememberDeviceUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine,
    private val environment: AuthEnvironment
) {
    suspend fun execute() {
        val username = stateMachine.requireSignedInState().signedInData.username
        val deviceId = environment.getDeviceMetadata(username)?.deviceKey
        val token = fetchAuthSession.execute().accessToken

        client.updateDeviceStatus {
            accessToken = token
            deviceKey = deviceId
            deviceRememberedStatus = DeviceRememberedStatusType.Remembered
        }
    }
}
