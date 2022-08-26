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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType

data class AuthChallenge(
    val challengeName: String,
    val username: String? = null,
    val session: String?,
    val parameters: Map<String, String>?
) {
    fun getChallengeResponseKey(): String? {
        val VALUE_ANSWER = "ANSWER"
        val VALUE_SMS_MFA = "SMS_MFA_CODE"
        val VALUE_NEW_PASSWORD = "NEW_PASSWORD"
        return when (ChallengeNameType.fromValue(challengeName)) {
            is ChallengeNameType.SmsMfa -> VALUE_SMS_MFA
            is ChallengeNameType.NewPasswordRequired -> VALUE_NEW_PASSWORD
            is ChallengeNameType.CustomChallenge -> VALUE_ANSWER
            else -> null
        }
    }
}
