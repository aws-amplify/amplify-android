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
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.plugin.PluginException;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of the {@link HubPlugin} which dispatches messages via
 * an {@link ExecutorService}.
 */
public final class BackgroundExecutorHubPlugin extends HubPlugin<Void> {

    private static final String TAG = BackgroundExecutorHubPlugin.class.getSimpleName();

    private final Map<SubscriptionToken, HubSubscription> subscriptionsByToken;
    private final Map<HubChannel, Set<HubSubscription>> subscriptionsByChannel;
    private final Object subscriptionsLock;

    private final ExecutorService executorService;
    private final Handler mainHandler;

    BackgroundExecutorHubPlugin() {
        this.subscriptionsByToken = new HashMap<>();
        this.subscriptionsByChannel = new HashMap<>();
        this.subscriptionsLock = new Object();
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void publish(@NonNull final HubChannel hubChannel, @NonNull final HubEvent hubEvent) {
        executorService.submit(() -> {
            final Set<HubSubscription> safeSubscriptions = new HashSet<>();
            synchronized (subscriptionsLock) {
                if (subscriptionsByChannel.containsKey(hubChannel)) {
                    safeSubscriptions.addAll(subscriptionsByChannel.get(hubChannel));
                }
            }

            for (HubSubscription subscription : safeSubscriptions) {
                if (subscription.getHubEventFilter() != null &&
                        !subscription.getHubEventFilter().filter(hubEvent)) {
                    continue;
                }

                mainHandler.post(() -> subscription.getHubSubscriber().onEvent(hubEvent));
            }
        });
    }

    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @NonNull HubSubscriber hubSubscriber) {
        return subscribe(hubChannel, null, hubSubscriber);
    }

    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubEventFilter hubEventFilter,
                                       @NonNull HubSubscriber hubSubscriber) {
        Objects.requireNonNull(hubChannel);
        Objects.requireNonNull(hubSubscriber);

        final SubscriptionToken token = SubscriptionToken.create();
        final HubSubscription hubSubscription =
            new HubSubscription(hubChannel, hubEventFilter, hubSubscriber);

        synchronized (subscriptionsLock) {
            subscriptionsByToken.put(token, hubSubscription);

            if (!subscriptionsByChannel.containsKey(hubChannel)) {
                subscriptionsByChannel.put(hubChannel, new HashSet<>());
            }
            subscriptionsByChannel.get(hubChannel).add(hubSubscription);
        }

        return token;
    }

    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) {
        synchronized (subscriptionsLock) {
            // First, find the subscription while trying to remove its subscription from the token map.
            HubSubscription subscriptionBeingEnded = subscriptionsByToken.remove(subscriptionToken);
            if (subscriptionBeingEnded == null) {
                throw new HubException("Invalid subscription token. Subscriber invalid, or already unsubscribed?");
            }

            // Now that we have a handle to the subscription, figure out which channel
            final HubChannel channelToUpdate = subscriptionBeingEnded.getHubChannel();
            if (subscriptionsByChannel.containsKey(channelToUpdate)) {
                subscriptionsByChannel.get(channelToUpdate).remove(subscriptionBeingEnded);
            }
        }
    }

    @Override
    public String getPluginKey() {
        return BackgroundExecutorHubPlugin.class.getSimpleName();
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException {

    }

    @Override
    public Void getEscapeHatch() {
        return null;
    }

    /**
     * Encapsulates information about a subscription.  This is needed so
     * that we can have O(1) lookup with a subscriptions map for
     * subscribe and unsubscribe(), but still be able to lookup the set
     * of subscribers for a channel in O(1), as well. Lastly, this
     * subscription object provides a reference to the optional event
     * filter which is evaluated when events are published.
     */
    static final class HubSubscription {
        private final HubChannel channel;
        private final HubEventFilter hubEventFilter;
        private final HubSubscriber hubSubscriber;

        HubSubscription(@NonNull final HubChannel channel,
                        @Nullable final HubEventFilter hubEventFilter,
                        @Nullable final HubSubscriber hubSubscriber) {
            this.channel = channel;
            this.hubEventFilter = hubEventFilter;
            this.hubSubscriber = hubSubscriber;
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
    }
}
