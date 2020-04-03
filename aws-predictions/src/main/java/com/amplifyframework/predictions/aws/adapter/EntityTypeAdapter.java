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

import com.amplifyframework.predictions.models.EntityType;

import java.util.Locale;

/**
 * Utility to convert AWS Comprehend's entity type
 * into Amplify-compatible data structure
 * (i.e. {@link EntityType}).
 */
public final class EntityTypeAdapter {
    @SuppressWarnings("checkstyle:all") private EntityTypeAdapter() {}

    /**
     * Converts the entity type string returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param entity Entity type returned by AWS Comprehend
     * @return Amplify's {@link EntityType} enum
     */
    @NonNull
    public static EntityType fromComprehend(@NonNull String entity) {
        switch (entity.toLowerCase(Locale.US)) {
            case "person":
                return EntityType.PERSON;
            case "location":
                return EntityType.LOCATION;
            case "organization":
                return EntityType.ORGANIZATION;
            case "commercial_item":
                return EntityType.COMMERCIAL_ITEM;
            case "event":
                return EntityType.EVENT;
            case "date":
                return EntityType.DATE;
            case "quantity":
                return EntityType.QUANTITY;
            case "title":
                return EntityType.TITLE;
            case "other":
            default:
                return EntityType.UNKNOWN;
        }
    }
}
