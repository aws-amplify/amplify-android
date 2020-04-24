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

import java.util.Locale;

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
        switch (emotion.toLowerCase(Locale.US)) {
            case "happy":
                return EmotionType.HAPPY;
            case "sad":
                return EmotionType.SAD;
            case "angry":
                return EmotionType.ANGRY;
            case "confused":
                return EmotionType.CONFUSED;
            case "disgusted":
                return EmotionType.DISGUSTED;
            case "surprised":
                return EmotionType.SURPRISED;
            case "calm":
                return EmotionType.CALM;
            case "fear":
                return EmotionType.FEAR;
            default:
                return EmotionType.UNKNOWN;
        }
    }
}
