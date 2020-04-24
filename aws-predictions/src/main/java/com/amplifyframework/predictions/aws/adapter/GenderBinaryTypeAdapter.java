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

import com.amplifyframework.predictions.models.GenderBinaryType;

import java.util.Locale;

/**
 * Utility to convert AWS Rekognition's gender type
 * into Amplify-compatible data structure
 * (i.e. {@link GenderBinaryType}).
 */
public final class GenderBinaryTypeAdapter {
    private GenderBinaryTypeAdapter() {}

    /**
     * Converts the gender type string returned by AWS Rekognition
     * into a format supported by Amplify Predictions.
     * @param gender Gender type returned by AWS Rekognition
     * @return Amplify's {@link GenderBinaryType} enum
     */
    @NonNull
    public static GenderBinaryType fromRekognition(@NonNull String gender) {
        switch (gender.toLowerCase(Locale.US)) {
            case "male":
                return GenderBinaryType.MALE;
            case "female":
                return GenderBinaryType.FEMALE;
            default:
                return GenderBinaryType.UNKNOWN;
        }
    }
}
