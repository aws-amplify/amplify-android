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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.BuildConfig;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of the {@link HubPlugin} which dispatches messages via
 * an {@link ExecutorService}.
 */
public final class AWSHubPlugin extends HubPlugin<Void> {
    private final Set<Subscription> subscriptions;
    private final ExecutorService executorService;

    /**
     * Constructs a new AWSHubPlugin.
     */
    @SuppressWarnings("WeakerAccess") // This is a public API
    public AWSHubPlugin() {
        this.subscriptions = new HashSet<>();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public <T> void publish(@NonNull HubChannel hubChannel, @NonNull HubEvent<T> hubEvent) {
        Objects.requireNonNull(hubChannel);
        Objects.requireNonNull(hubEvent);
        executorService.execute(() -> {
            synchronized (subscriptions) {
                for (Subscription subscription : subscriptions) {
                    if (subscription.getHubChannel().equals(hubChannel) &&
                            subscription.getHubEventFilter().filter(hubEvent)) {
                        executorService.execute(() -> subscription.getHubSubscriber().onEvent(hubEvent));
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @NonNull HubSubscriber hubSubscriber) {
        return subscribe(hubChannel, HubEventFilters.always(), hubSubscriber);
    }

    @NonNull
    @Override
    public SubscriptionToken subscribe(
            @NonNull HubChannel hubChannel,
            @NonNull HubEventFilter hubEventFilter,
            @NonNull HubSubscriber hubSubscriber) {
        Objects.requireNonNull(hubChannel);
        Objects.requireNonNull(hubEventFilter);
        Objects.requireNonNull(hubSubscriber);
        SubscriptionToken token = SubscriptionToken.create();
        synchronized (subscriptions) {
            subscriptions.add(new Subscription(token, hubChannel, hubEventFilter, hubSubscriber));
        }
        return token;
    }

    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) {
        Objects.requireNonNull(subscriptionToken);
        synchronized (subscriptions) {
            Iterator<Subscription> iterator = subscriptions.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getSubscriptionToken().equals(subscriptionToken)) {
                    iterator.remove();
                }
            }
        }
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "awsHubPlugin";
    }

    @Override
    public void configure(JSONObject pluginConfiguration, @NonNull Context context) {
    }

    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Encapsulates information about a subscription.
     */
    static final class Subscription {
        private final SubscriptionToken subscriptionToken;
        private final HubChannel channel;
        private final HubEventFilter hubEventFilter;
        private final HubSubscriber hubSubscriber;

        Subscription(
                @NonNull SubscriptionToken subscriptionToken,
                @NonNull HubChannel channel,
                @NonNull HubEventFilter hubEventFilter,
                @NonNull HubSubscriber hubSubscriber) {
            this.subscriptionToken = Objects.requireNonNull(subscriptionToken);
            this.channel = Objects.requireNonNull(channel);
            this.hubEventFilter = Objects.requireNonNull(hubEventFilter);
            this.hubSubscriber = Objects.requireNonNull(hubSubscriber);
        }

        SubscriptionToken getSubscriptionToken() {
            return subscriptionToken;
        }

        HubChannel getHubChannel() {
            return channel;
        }

        HubEventFilter getHubEventFilter() {
            return hubEventFilter;
        }

        HubSubscriber getHubSubscriber() {
            return hubSubscriber;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Subscription that = (Subscription) thatObject;

            if (!ObjectsCompat.equals(subscriptionToken, that.subscriptionToken)) {
                return false;
            }
            if (channel != that.channel) {
                return false;
            }
            if (!ObjectsCompat.equals(hubEventFilter, that.hubEventFilter)) {
                return false;
            }
            return ObjectsCompat.equals(hubSubscriber, that.hubSubscriber);
        }

        @Override
        public int hashCode() {
            int result = subscriptionToken.hashCode();
            result = 31 * result + channel.hashCode();
            result = 31 * result + hubEventFilter.hashCode();
            result = 31 * result + hubSubscriber.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Subscription{" +
                "subscriptionToken=" + subscriptionToken +
                ", channel=" + channel +
                ", hubEventFilter=" + hubEventFilter +
                ", hubSubscriber=" + hubSubscriber +
                '}';
        }
    }
}
