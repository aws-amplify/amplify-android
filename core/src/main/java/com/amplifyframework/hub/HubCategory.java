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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

/**
 * Amplify has a local eventing system called Hub. It is a lightweight implementation of
 * Publisher-Subscriber pattern, and is used to share data between modules and components
 * in your app. Amplify uses Hub for different categories to communicate with one another
 * when specific events occur, such as authentication events like a user sign-in or
 * notification of a file download.
 */
public final class HubCategory extends Category<HubPlugin<?>> implements HubCategoryBehavior {
    private final HubPlugin<?> defaultPlugin;

    /**
     * Constructs a Hub Category.
     */
    public HubCategory() {
        super();
        this.defaultPlugin = new AWSHubPlugin();
    }

    @Override
    public <T> void publish(@NonNull HubChannel hubChannel, @NonNull HubEvent<T> hubEvent) {
        getHubPlugin().publish(hubChannel, hubEvent);
    }

    @NonNull
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @NonNull HubSubscriber hubSubscriber) {
        return getHubPlugin().subscribe(hubChannel, hubSubscriber);
    }

    @NonNull
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubEventFilter hubEventFilter,
                                       @NonNull HubSubscriber hubSubscriber) {
        return getHubPlugin().subscribe(hubChannel, hubEventFilter, hubSubscriber);
    }

    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) {
        getHubPlugin().unsubscribe(subscriptionToken);
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.HUB;
    }

    private HubPlugin<?> getHubPlugin() {
        if (!super.isInitialized() || super.getPlugins().isEmpty()) {
            return defaultPlugin;
        } else {
            return super.getSelectedPlugin();
        }
    }
}
