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

import aws.sdk.kotlin.services.rekognition.model.EmotionName
import com.amplifyframework.predictions.models.EmotionType

/**
 * Utility to convert AWS Rekognition's emotion type
 * into Amplify-compatible data structure
 * (i.e. [EmotionType]).
 */
object EmotionTypeAdapter {
    /**
     * Converts the emotion type string returned by AWS Rekognition
     * into a format supported by Amplify Predictions.
     * @param emotion the emotion type returned by AWS Rekognition
     * @return Amplify's [EmotionType] enum
     */
    fun fromRekognition(emotion: String): EmotionType {
        return when (EmotionName.fromValue(emotion)) {
            EmotionName.Happy -> EmotionType.HAPPY
            EmotionName.Sad -> EmotionType.SAD
            EmotionName.Angry -> EmotionType.ANGRY
            EmotionName.Confused -> EmotionType.CONFUSED
            EmotionName.Disgusted -> EmotionType.DISGUSTED
            EmotionName.Surprised -> EmotionType.SURPRISED
            EmotionName.Calm -> EmotionType.CALM
            EmotionName.Fear -> EmotionType.FEAR
            else -> EmotionType.UNKNOWN
        }
    }
}
