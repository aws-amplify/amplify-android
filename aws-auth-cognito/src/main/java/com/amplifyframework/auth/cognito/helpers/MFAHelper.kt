/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.ChallengeParameter
import com.amplifyframework.statemachine.codegen.data.challengeNameType

@Throws(IllegalArgumentException::class)
internal fun getMFAType(value: String) = when (value) {
    "SMS_MFA" -> MFAType.SMS
    "SOFTWARE_TOKEN_MFA" -> MFAType.TOTP
    "EMAIL_OTP" -> MFAType.EMAIL
    else -> throw IllegalArgumentException("Unsupported MFA type")
}

internal fun getMFATypeOrNull(value: String) = when (value) {
    "SMS_MFA" -> MFAType.SMS
    "SOFTWARE_TOKEN_MFA" -> MFAType.TOTP
    "EMAIL_OTP" -> MFAType.EMAIL
    else -> null
}

internal fun getMFASetupTypeOrNull(value: String) = when (value) {
    "SOFTWARE_TOKEN_MFA" -> MFAType.TOTP
    "EMAIL_OTP" -> MFAType.EMAIL
    else -> null
}

internal val MFAType.value: String
    get() = when (this) {
        MFAType.SMS -> "SMS_MFA"
        MFAType.TOTP -> "SOFTWARE_TOKEN_MFA"
        MFAType.EMAIL -> "EMAIL_OTP"
    }

internal fun isMfaSetupSelectionChallenge(challenge: AuthChallenge) =
    challenge.challengeNameType == ChallengeNameType.MfaSetup &&
        getAllowedMFASetupTypesFromChallengeParameters(challenge.parameters).size > 1

internal fun isEmailMfaSetupChallenge(challenge: AuthChallenge) =
    challenge.challengeNameType == ChallengeNameType.MfaSetup &&
        getAllowedMFASetupTypesFromChallengeParameters(challenge.parameters) == setOf(MFAType.EMAIL)

internal fun getAllowedMFATypesFromChallengeParameters(challengeParameters: Map<String, String>?): Set<MFAType> {
    val mfasCanChoose = challengeParameters?.get(ChallengeParameter.MfasCanChoose.key) ?: return emptySet()
    val result = mutableSetOf<MFAType>()
    mfasCanChoose.replace(Regex("\\[|\\]|\""), "").split(",").forEach {
        when (it) {
            "SMS_MFA" -> result.add(MFAType.SMS)
            "SOFTWARE_TOKEN_MFA" -> result.add(MFAType.TOTP)
            "EMAIL_OTP" -> result.add(MFAType.EMAIL)
            else -> throw UnknownException(cause = Exception("MFA type not supported."))
        }
    }
    return result
}

// We exclude SMS as a setup type
internal fun getAllowedMFASetupTypesFromChallengeParameters(challengeParameters: Map<String, String>?): Set<MFAType> {
    val mfasCanSetup = challengeParameters?.get(ChallengeParameter.MfasCanSetup.key) ?: return emptySet()

    val result = mutableSetOf<MFAType>()
    mfasCanSetup.replace(Regex("\\[|\\]|\""), "").split(",").forEach {
        when (it) {
            "SOFTWARE_TOKEN_MFA" -> result.add(MFAType.TOTP)
            "EMAIL_OTP" -> result.add(MFAType.EMAIL)
        }
    }
    return result
}
