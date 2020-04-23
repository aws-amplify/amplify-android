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
        switch (landmark) {
            case "eyeLeft":
            case "leftEyeLeft":
            case "leftEyeRight":
            case "leftEyeUp":
            case "leftEyeDown":
                return LandmarkType.LEFT_EYE;
            case "eyeRight":
            case "rightEyeLeft":
            case "rightEyeRight":
            case "rightEyeUp":
            case "rightEyeDown":
                return LandmarkType.RIGHT_EYE;
            case "leftEyeBrowLeft":
            case "leftEyeBrowRight":
            case "leftEyeBrowUp":
                return LandmarkType.LEFT_EYEBROW;
            case "rightEyeBrowLeft":
            case "rightEyeBrowRight":
            case "rightEyeBrowUp":
                return LandmarkType.RIGHT_EYEBROW;
            case "nose":
                return LandmarkType.NOSE;
            case "noseLeft":
            case "noseRight":
                return LandmarkType.NOSE_CREST;
            case "mouthLeft":
            case "mouthRight":
            case "mouthUp":
            case "mouthDown":
                return LandmarkType.OUTER_LIPS;
            case "leftPupil":
                return LandmarkType.LEFT_PUPIL;
            case "rightPupil":
                return LandmarkType.RIGHT_PUPIL;
            case "upperJawlineLeft":
            case "midJawlineLeft":
            case "chinBottom":
            case "midJawlineRight":
            case "upperJawlineRight":
                return LandmarkType.FACE_CONTOUR;
            default:
                return LandmarkType.UNKNOWN;
        }
    }
}
