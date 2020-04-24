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

import com.amplifyframework.predictions.models.LandmarkType;

/**
 * Utility to convert Amazon Rekognition's entity landmark type
 * into Amplify-compatible data structure (i.e. {@link LandmarkType}).
 */
public final class LandmarkTypeAdapter {
    private LandmarkTypeAdapter() {}

    /**
     * Converts the landmark type string returned by Amazon Rekognition
     * into a format supported by Amplify Predictions.
     * @param landmark Landmark type returned by Amazon Rekognition
     * @return Amplify's {@link LandmarkType} enum
     */
    @NonNull
    public static LandmarkType fromRekognition(@NonNull String landmark) {
        com.amazonaws.services.rekognition.model.LandmarkType type =
                com.amazonaws.services.rekognition.model.LandmarkType.fromValue(landmark);
        switch (type) {
            case EyeLeft:
            case LeftEyeLeft:
            case LeftEyeRight:
            case LeftEyeUp:
            case LeftEyeDown:
                return LandmarkType.LEFT_EYE;
            case EyeRight:
            case RightEyeLeft:
            case RightEyeRight:
            case RightEyeUp:
            case RightEyeDown:
                return LandmarkType.RIGHT_EYE;
            case LeftEyeBrowLeft:
            case LeftEyeBrowRight:
            case LeftEyeBrowUp:
                return LandmarkType.LEFT_EYEBROW;
            case RightEyeBrowLeft:
            case RightEyeBrowRight:
            case RightEyeBrowUp:
                return LandmarkType.RIGHT_EYEBROW;
            case Nose:
                return LandmarkType.NOSE;
            case NoseLeft:
            case NoseRight:
                return LandmarkType.NOSE_CREST;
            case MouthLeft:
            case MouthRight:
            case MouthUp:
            case MouthDown:
                return LandmarkType.OUTER_LIPS;
            case LeftPupil:
                return LandmarkType.LEFT_PUPIL;
            case RightPupil:
                return LandmarkType.RIGHT_PUPIL;
            case UpperJawlineLeft:
            case MidJawlineLeft:
            case ChinBottom:
            case MidJawlineRight:
            case UpperJawlineRight:
                return LandmarkType.FACE_CONTOUR;
            default:
                return LandmarkType.UNKNOWN;
        }
    }
}
