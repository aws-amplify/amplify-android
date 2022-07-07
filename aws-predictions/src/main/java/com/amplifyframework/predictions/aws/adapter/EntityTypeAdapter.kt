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

import com.amplifyframework.predictions.models.EntityType

/**
 * Utility to convert AWS Comprehend's entity type
 * into Amplify-compatible data structure
 * (i.e. [EntityType]).
 */
object EntityTypeAdapter {
    /**
     * Converts the entity type string returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param entity Entity type returned by AWS Comprehend
     * @return Amplify's [EntityType] enum
     */
    @JvmStatic
    fun fromComprehend(entity: String): EntityType {
        return when (aws.sdk.kotlin.services.comprehend.model.EntityType.fromValue(entity)) {
            aws.sdk.kotlin.services.comprehend.model.EntityType.Person -> EntityType.PERSON
            aws.sdk.kotlin.services.comprehend.model.EntityType.Location -> EntityType.LOCATION
            aws.sdk.kotlin.services.comprehend.model.EntityType.Organization -> EntityType.ORGANIZATION
            aws.sdk.kotlin.services.comprehend.model.EntityType.CommercialItem -> EntityType.COMMERCIAL_ITEM
            aws.sdk.kotlin.services.comprehend.model.EntityType.Event -> EntityType.EVENT
            aws.sdk.kotlin.services.comprehend.model.EntityType.Date -> EntityType.DATE
            aws.sdk.kotlin.services.comprehend.model.EntityType.Quantity -> EntityType.QUANTITY
            aws.sdk.kotlin.services.comprehend.model.EntityType.Title -> EntityType.TITLE
            aws.sdk.kotlin.services.comprehend.model.EntityType.Other -> EntityType.UNKNOWN
            else -> EntityType.UNKNOWN
        }
    }
}
