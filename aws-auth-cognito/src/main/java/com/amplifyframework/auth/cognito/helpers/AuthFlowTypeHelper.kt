/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.auth.cognito.helpers

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType as CognitoAuthFlowType
import com.amplifyframework.auth.cognito.options.AuthFlowType

internal fun AuthFlowType.toCognitoType() = when (this) {
    AuthFlowType.USER_SRP_AUTH -> CognitoAuthFlowType.UserSrpAuth
    AuthFlowType.CUSTOM_AUTH -> CognitoAuthFlowType.CustomAuth
    AuthFlowType.CUSTOM_AUTH_WITH_SRP -> CognitoAuthFlowType.CustomAuth
    AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP -> CognitoAuthFlowType.CustomAuth
    AuthFlowType.USER_PASSWORD_AUTH -> CognitoAuthFlowType.UserPasswordAuth
    AuthFlowType.USER_AUTH -> CognitoAuthFlowType.UserAuth
}
