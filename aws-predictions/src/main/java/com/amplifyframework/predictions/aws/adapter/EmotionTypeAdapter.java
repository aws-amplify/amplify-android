/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.adapter;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.EmotionType;

import com.amazonaws.services.rekognition.model.EmotionName;

/**
 * Utility to convert AWS Rekognition's emotion type
 * into Amplify-compatible data structure
 * (i.e. {@link EmotionType}).
 */
public final class EmotionTypeAdapter {
    private EmotionTypeAdapter() {}

    /**
     * Converts the emotion type string returned by AWS Rekognition
     * into a format supported by Amplify Predictions.
     * @param emotion the emotion type returned by AWS Rekognition
     * @return Amplify's {@link EmotionType} enum
     */
    @NonNull
    public static EmotionType fromRekognition(@NonNull String emotion) {
        EmotionName type = EmotionName.fromValue(emotion);
        switch (type) {
            case HAPPY:
                return EmotionType.HAPPY;
            case SAD:
                return EmotionType.SAD;
            case ANGRY:
                return EmotionType.ANGRY;
            case CONFUSED:
                return EmotionType.CONFUSED;
            case DISGUSTED:
                return EmotionType.DISGUSTED;
            case SURPRISED:
                return EmotionType.SURPRISED;
            case CALM:
                return EmotionType.CALM;
            case FEAR:
                return EmotionType.FEAR;
            default:
                return EmotionType.UNKNOWN;
        }
    }
}
