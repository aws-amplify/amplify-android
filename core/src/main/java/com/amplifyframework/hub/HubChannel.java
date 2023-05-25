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

package com.amplifyframework.hub;

import com.amplifyframework.core.category.CategoryType;

/**
 * HubChannel represents the channels on which Amplify category messages will be dispatched.
 * Apps can define their own channels for intra-app communication. Internally, Amplify uses the Hub
 * for dispatching notifications about events associated with different categories.
 */
public enum HubChannel {
    /**
     * Hub messages relating to Amplify Analytics.
     */
    ANALYTICS(CategoryType.ANALYTICS),

    /**
     * Hub messages relating to Amplify Api.
     */
    API(CategoryType.API),

    /**
     * Hub messages relating to Amplify Auth.
     */
    AUTH(CategoryType.AUTH),

    /**
     * Hub messages relating to Amplify DataStore.
     */
    DATASTORE(CategoryType.DATASTORE),

    /**
     * Hub messages relating to Amplify Geo.
     */
    GEO(CategoryType.GEO),

    /**
     * Hub messages relating to Amplify Hub.
     */
    HUB(CategoryType.HUB),

    /**
     * Hub messages relating to Amplify Logging.
     */
    LOGGING(CategoryType.LOGGING),

    /**
     * Hub messages relating to Amplify Predictions.
     */
    PREDICTIONS(CategoryType.PREDICTIONS),

    /**
     * Hub messages relating to Amplify Notifications.
     */
    NOTIFICATIONS(CategoryType.NOTIFICATIONS),

    /**
     * Hub messages relating to Amplify Storage.
     */
    STORAGE(CategoryType.STORAGE);

    private final CategoryType categoryType;

    HubChannel(CategoryType categoryType) {
        this.categoryType = categoryType;
    }

    /**
     * Look up HubChannel based on CategoryType.
     * @param categoryType identifies an Amplify category
     * @return the hub channel corresponding to it
     */
    public static HubChannel forCategoryType(final CategoryType categoryType) {
        for (final HubChannel possibleMatch : values()) {
            if (possibleMatch.categoryType.equals(categoryType)) {
                return possibleMatch;
            }
        }
        throw new IllegalArgumentException("No HubChannel found for the CategoryType: " + categoryType);
    }
}

