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

package com.amplifyframework.predictions.aws.models;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.aws.AWSPredictionsPlugin;
import com.amplifyframework.predictions.models.BinaryFeature;
import com.amplifyframework.predictions.models.EntityDetails;

/**
 * The list of binary features that can be detected by {@link AWSPredictionsPlugin}
 * when detecting entities from an image.
 *
 * Use {@link BinaryFeature#getTypeAlias()} to check the corresponding binary feature
 * value from an instance of {@link EntityDetails}.
 */
public enum BinaryFeatureType {
    /**
     * Detected entity is bearded.
     */
    BEARD("Beard"),

    /**
     * Detected entity wears sunglasses.
     */
    SUNGLASSES("Sunglasses"),

    /**
     * Detected entity is smiling.
     */
    SMILE("Smile"),

    /**
     * Detected entity wears eye glasses.
     */
    EYE_GLASSES("EyeGlasses"),

    /**
     * Detected entity has a mustache.
     */
    MUSTACHE("Mustache"),

    /**
     * Detected entity is opening its mouth.
     */
    MOUTH_OPEN("MouthOpen"),

    /**
     * Detected entity is opening its eyes.
     */
    EYES_OPEN("EyesOpen");

    private final String typeAlias;

    BinaryFeatureType(String typeAlias) {
        this.typeAlias = typeAlias;
    }

    /**
     * Gets the name of the given feature type.
     */
    @NonNull
    public String getAlias() {
        return typeAlias;
    }
}
