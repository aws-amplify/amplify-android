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
class FaceLivenessSessionInformation {
    val videoWidth: Float
    val videoHeight: Float
    val challengeVersions: List<Challenge>
    val region: String
    val preCheckViewEnabled: Boolean?
    val attemptCount: Int?

    @Deprecated("Keeping compatibility for <= Amplify Liveness 1.2.6")
    constructor(
        videoWidth: Float,
        videoHeight: Float,
        challenge: String,
        region: String
    ) {
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
        this.challengeVersions = listOf(Challenge.FaceMovementAndLightChallenge("1.0.0"))
        this.region = region
        this.preCheckViewEnabled = null
        this.attemptCount = null
    }

    constructor(
        videoWidth: Float,
        videoHeight: Float,
        region: String,
        challengeVersions: List<Challenge>,
        preCheckViewEnabled: Boolean,
        attemptCount: Int
    ) {
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
        this.region = region
        this.challengeVersions = challengeVersions
        this.preCheckViewEnabled = preCheckViewEnabled
        this.attemptCount = attemptCount
    }
}

@InternalAmplifyApi
sealed class Challenge private constructor(val name: String, val version: String) {

    @InternalAmplifyApi
    class FaceMovementChallenge(version: String) : Challenge("FaceMovementChallenge", version)

    @InternalAmplifyApi
    class FaceMovementAndLightChallenge(version: String) : Challenge("FaceMovementAndLightChallenge", version)

    fun compareType(challenge: Challenge): Boolean {
        return this.name == challenge.name && this.version == challenge.version
    }

    fun toQueryParamString(): String = "${name}_$version"
}
