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
package com.amplifyframework.predictions.models

import com.amplifyframework.annotations.InternalAmplifyApi

@InternalAmplifyApi
data class FaceLivenessSessionInformation(
    val videoWidth: Float,
    val videoHeight: Float,
    val challenge: String? = null,
    val region: String,
    val challengeVersions: List<Challenge>? = null,
    val preCheckViewEnabled: Boolean? = null,
    val attemptCount: Int? = null
)

@InternalAmplifyApi
data class Challenge(
    val type: ChallengeType,
    val version: String
) {
    fun toQueryParamString(): String = "${type.name}_$version"
}

@InternalAmplifyApi
enum class ChallengeType {
    FaceMovementChallenge,
    FaceMovementAndLightChallenge,
}
