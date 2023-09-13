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
package com.amplifyframework.predictions.aws.models.liveness

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChallengeConfig(
    @SerialName("BlazeFaceDetectionThreshold") val blazeFaceDetectionThreshold: Float,
    @SerialName("FaceDistanceThresholdMin") val faceDistanceThresholdMin: Float,
    @SerialName("FaceDistanceThreshold") val faceDistanceThreshold: Float,
    @SerialName("FaceDistanceThresholdMax") val faceDistanceThresholdMax: Float,
    @SerialName("OvalIouThreshold") val ovalIouThreshold: Float,
    @SerialName("OvalHeightWidthRatio") val ovalHeightWidthRatio: Float,
    @SerialName("OvalIouWidthThreshold") val ovalIouWidthThreshold: Float,
    @SerialName("OvalIouHeightThreshold") val ovalIouHeightThreshold: Float,
    @SerialName("FaceIouWidthThreshold") val faceIouWidthThreshold: Float,
    @SerialName("FaceIouHeightThreshold") val faceIouHeightThreshold: Float,
    @SerialName("OvalFitTimeout") val ovalFitTimeout: Int
)
