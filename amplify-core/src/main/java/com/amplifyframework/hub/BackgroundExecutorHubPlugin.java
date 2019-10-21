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

    /**
     * Dispatch a Hub message on the specified channel.
     * @param hubChannel The channel to send the message on
     * @param hubPayload The payload to send
     */
    @Override
    public void publish(@NonNull final HubChannel hubChannel, @NonNull final HubPayload hubPayload) {
        executorService.submit(() -> {
            final Set<HubSubscription> safeSubscriptions = new HashSet<>();
            synchronized (subscriptionsLock) {
                if (subscriptionsByChannel.containsKey(hubChannel)) {
                    //noinspection ConstantConditions contains is true & our impl never puts() a null value.
                    safeSubscriptions.addAll(subscriptionsByChannel.get(hubChannel));
                }
            }

            for (HubSubscription subscription : safeSubscriptions) {
                if (subscription.getHubPayloadFilter() != null &&
                        !subscription.getHubPayloadFilter().filter(hubPayload)) {
                    continue;
                }

                mainHandler.post(() -> subscription.getHubListener().onEvent(hubPayload));
            }
        });
    }

    /**
     * Listen to Hub messages on a particular channel.
     * @param hubChannel The channel to listen for messages on
     * @param listener   The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with
     * {@link #unsubscribe(SubscriptionToken)}
     * to de-register the listener.
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @NonNull HubListener listener) {
        return subscribe(hubChannel, null, listener);
    }

    /**
     * Listen to Hub messages on a particular channel.
     * @param hubChannel The channel to listen for messages on
     * @param hubPayloadFilter  candidate messages will be passed to this closure prior to dispatching to
     *                   the {@link HubListener}. Only messages for which the closure returns
     *                   `true` will be dispatched.
     * @param listener   The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with #unsubscribe(SubscriptionToken)
     * to de-register the listener.
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel,
                                       @Nullable HubPayloadFilter hubPayloadFilter,
                                       @NonNull HubListener listener) {
        Objects.requireNonNull(hubChannel);
        Objects.requireNonNull(listener);

        final SubscriptionToken token = SubscriptionToken.create();
        final HubSubscription hubSubscription =
            new HubSubscription(hubChannel, hubPayloadFilter, listener);

        synchronized (subscriptionsLock) {
            subscriptionsByToken.put(token, hubSubscription);

            if (!subscriptionsByChannel.containsKey(hubChannel)) {
                subscriptionsByChannel.put(hubChannel, new HashSet<>());
            }
            //noinspection ConstantConditions syncrhonized, and just ensured non-null via put(..., non-null).
            subscriptionsByChannel.get(hubChannel).add(hubSubscription);
        }

        return token;
    }

    /**
     * A subscribed listener can be removed from the Hub system by passing the
     * token received from {@link #subscribe(HubChannel, HubListener)} or
     * {@link #subscribe(HubChannel, HubPayloadFilter, HubListener)}.
     * @param subscriptionToken the token which serves as an identifier for the listener
     *                         {@link HubListener} registered
     */
    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) {
        synchronized (subscriptionsLock) {
            // First, find the listener while trying to remove its subscription from the token map.
            HubSubscription subscriptionBeingEnded = subscriptionsByToken.remove(subscriptionToken);
            if (subscriptionBeingEnded == null) {
                throw new HubException("Invalid subscription token. Listener invalid, or already unsubscribed?");
            }

            // Now that we have a handle to the subscription, figure out which channel
            final HubChannel channelToUpdate = subscriptionBeingEnded.getHubChannel();
            if (subscriptionsByChannel.containsKey(channelToUpdate)) {
                //noinspection ConstantConditions syncrhonized, key is valid, and we never put null values in map
                subscriptionsByChannel.get(channelToUpdate).remove(subscriptionBeingEnded);
            }
        }
    }

    /**
     * Gets the key for this plugin.
     * @return An identifier that uniquely identifies this plugin implementation
     */
    @Override
    public String getPluginKey() {
        return BackgroundExecutorHubPlugin.class.getSimpleName();
    }

    /**
     * Configure the plugin with customized configuration object.
     * @param pluginConfiguration plugin-specific configuration
     * @throws PluginException when configuration for a plugin was not found
     */
    @Override
    public void configure(@NonNull JSONObject pluginConfiguration, Context context) throws PluginException {

    }

    /**
     * Returns escape hatch for plugin to enable lower-level client use-cases.
     * @return the client used by category plugin
     */
    @Override
    public Void getEscapeHatch() {
        return null;
    }

    /**
     * Encapsulates information about a subscription.
     * This is needed so that we can have O(1) lookup with a subscriptions map
     * for subscribe and unsubscribe(), but still be able to lookup the set of
     * listeners for a channel in O(1), as well. Lastly, this subscription
     * object provides a reference to the optional payload filter which is evaluated
     * when payloads are published.
     */
    static final class HubSubscription {
        private final HubChannel channel;
        private final HubPayloadFilter hubPayloadFilter;
        private final HubListener hubListener;

        HubSubscription(@NonNull final HubChannel channel,
                        @Nullable final HubPayloadFilter hubPayloadFilter,
                        @Nullable final HubListener hubListener) {
            this.channel = channel;
            this.hubPayloadFilter = hubPayloadFilter;
            this.hubListener = hubListener;
        }

        HubChannel getHubChannel() {
            return channel;
        }

        HubPayloadFilter getHubPayloadFilter() {
            return hubPayloadFilter;
        }

        HubListener getHubListener() {
            return hubListener;
        }
    }
}
