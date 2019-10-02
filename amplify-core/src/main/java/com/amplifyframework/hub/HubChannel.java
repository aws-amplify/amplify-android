/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.support.annotation.NonNull;

import com.amplifyframework.core.category.CategoryType;

import java.util.HashSet;
import java.util.Set;

/**
 * HubChannel represents the channels on which Amplify category messages will be dispatched.
 * Apps can define their own channels for intra-app communication. Internally, Amplify uses the Hub
 * for dispatching notifications about events associated with different categories.
 */
public enum HubChannel {
    /**
     * Hub messages relating to Amplify Analytics
     */
    ANALYTICS,

    /**
     * Hub messages relating to Amplify Api
     */
    API,

    /**
     * Hub messages relating to Amplify Hub
     */
    HUB,

    /**
     * Hub messages relating to Amplify Logging
     */
    LOGGING,

    /**
     * Hub messages relating to Amplify Storage
     */
    STORAGE,

    /**
     * A custom channel with its own name
     */
    CUSTOM("myCustomChannel")
    ;

    private String channelName;

    public static Set<HubChannel> amplifyChannels() {
        Set<HubChannel> channels = new HashSet<HubChannel>();
        for (CategoryType categoryType: CategoryType.values()) {
            channels.add(fromCategoryType(categoryType));
        }
        return channels;
    }

    public static HubChannel fromCategoryType(final CategoryType categoryType) {
        HubChannel hubChannel = null;
        if (CategoryType.ANALYTICS.equals(categoryType)) {
            hubChannel = HubChannel.ANALYTICS;
        } else if (CategoryType.API.equals(categoryType)) {
            hubChannel = HubChannel.API;
        } else if (CategoryType.HUB.equals(categoryType)) {
            hubChannel = HubChannel.HUB;
        } else if (CategoryType.LOGGING.equals(categoryType)) {
            hubChannel = HubChannel.LOGGING;
        } else if (CategoryType.STORAGE.equals(categoryType)) {
            hubChannel = HubChannel.STORAGE;
        }
        return hubChannel;
    }

    HubChannel() {

    }

    HubChannel(@NonNull final String channelName) {
        this.channelName = channelName;
    }
}
