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

package com.amplifyframework.statemachine.codegen.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SignInMethod {

    @Serializable
    @SerialName("SignInMethod.ApiBased")
    data class ApiBased(val authType: AuthType) : SignInMethod() {

        // Directly maps to AuthFlowType in Cognito Library
        enum class AuthType {
            USER_SRP_AUTH,
            CUSTOM_AUTH,
            USER_PASSWORD_AUTH
        }
    }

    @Serializable
    @SerialName("SignInMethod.HostedUI")
    data class HostedUI(val browserPackage: String? = null) : SignInMethod()
}
