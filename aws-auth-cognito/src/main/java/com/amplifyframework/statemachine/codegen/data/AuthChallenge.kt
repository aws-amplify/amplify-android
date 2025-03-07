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
import com.amplifyframework.statemachine.util.mask
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthChallenge(
    val challengeName: String,
    val username: String? = null,
    val session: String?,
    val parameters: Map<String, String>?,
    val availableChallenges: List<String>? = null
) {
    override fun toString(): String = "AuthChallenge(" +
        "challengeName='$challengeName', " +
        "username=$username, " +
        "session=${session.mask()}, " +
        "parameters=${parameters?.maskSensitiveChallengeParameters()}, " +
        "availableChallenges=$availableChallenges" +
        ")"
}

internal val AuthChallenge.challengeNameType
    get() = ChallengeNameType.fromValue(challengeName)

internal fun AuthChallenge.getParameter(parameter: ChallengeParameter) = parameters?.get(parameter.key)

internal fun Map<String, String>.maskSensitiveChallengeParameters() = mask(
    ChallengeParameter.CodeDeliveryDestination.key,
    ChallengeParameter.CredentialRequestOptions.key
)
