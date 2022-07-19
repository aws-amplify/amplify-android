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
package com.amplifyframework.predictions.aws.adapter

import aws.sdk.kotlin.services.rekognition.model.LandmarkType

/**
 * Utility to convert Amazon Rekognition's entity landmark type
 * into Amplify-compatible data structure (i.e. [LandmarkType]).
 */
object LandmarkTypeAdapter {
    /**
     * Converts the landmark type string returned by Amazon Rekognition
     * into a format supported by Amplify Predictions.
     * @param landmark Landmark type returned by Amazon Rekognition
     * @return Amplify's [LandmarkType] enum
     */
    @JvmStatic
    fun fromRekognition(landmark: String): com.amplifyframework.predictions.models.LandmarkType {
        return when (LandmarkType.fromValue(landmark)) {
            LandmarkType.EyeLeft, LandmarkType.LeftEyeLeft, LandmarkType.LeftEyeRight, LandmarkType.LeftEyeUp,
            LandmarkType.LeftEyeDown -> com.amplifyframework.predictions.models.LandmarkType.LEFT_EYE
            LandmarkType.EyeRight, LandmarkType.RightEyeLeft, LandmarkType.RightEyeRight, LandmarkType.RightEyeUp,
            LandmarkType.RightEyeDown -> com.amplifyframework.predictions.models.LandmarkType.RIGHT_EYE
            LandmarkType.LeftEyeBrowLeft, LandmarkType.LeftEyeBrowRight, LandmarkType.LeftEyeBrowUp ->
                com.amplifyframework.predictions.models.LandmarkType.LEFT_EYEBROW
            LandmarkType.RightEyeBrowLeft, LandmarkType.RightEyeBrowRight, LandmarkType.RightEyeBrowUp ->
                com.amplifyframework.predictions.models.LandmarkType.RIGHT_EYEBROW
            LandmarkType.Nose -> com.amplifyframework.predictions.models.LandmarkType.NOSE
            LandmarkType.NoseLeft, LandmarkType.NoseRight ->
                com.amplifyframework.predictions.models.LandmarkType.NOSE_CREST
            LandmarkType.MouthLeft, LandmarkType.MouthRight, LandmarkType.MouthUp, LandmarkType.MouthDown ->
                com.amplifyframework.predictions.models.LandmarkType.OUTER_LIPS
            LandmarkType.LeftPupil -> com.amplifyframework.predictions.models.LandmarkType.LEFT_PUPIL
            LandmarkType.RightPupil -> com.amplifyframework.predictions.models.LandmarkType.RIGHT_PUPIL
            LandmarkType.UpperJawlineLeft, LandmarkType.MidJawlineLeft, LandmarkType.ChinBottom,
            LandmarkType.MidJawlineRight, LandmarkType.UpperJawlineRight ->
                com.amplifyframework.predictions.models.LandmarkType.FACE_CONTOUR
            else -> com.amplifyframework.predictions.models.LandmarkType.UNKNOWN
        }
    }
}
