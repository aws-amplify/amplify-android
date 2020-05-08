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

/**
 * Utility to convert AWS Comprehend's entity type
 * into Amplify-compatible data structure
 * (i.e. {@link EntityType}).
 */
public final class EntityTypeAdapter {
    private EntityTypeAdapter() {}

    /**
     * Converts the entity type string returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param entity Entity type returned by AWS Comprehend
     * @return Amplify's {@link EntityType} enum
     */
    @NonNull
    public static EntityType fromComprehend(@NonNull String entity) {
        com.amazonaws.services.comprehend.model.EntityType type =
                com.amazonaws.services.comprehend.model.EntityType.fromValue(entity);
        switch (type) {
            case PERSON:
                return EntityType.PERSON;
            case LOCATION:
                return EntityType.LOCATION;
            case ORGANIZATION:
                return EntityType.ORGANIZATION;
            case COMMERCIAL_ITEM:
                return EntityType.COMMERCIAL_ITEM;
            case EVENT:
                return EntityType.EVENT;
            case DATE:
                return EntityType.DATE;
            case QUANTITY:
                return EntityType.QUANTITY;
            case TITLE:
                return EntityType.TITLE;
            case OTHER:
            default:
                return EntityType.UNKNOWN;
        }
    }
}
