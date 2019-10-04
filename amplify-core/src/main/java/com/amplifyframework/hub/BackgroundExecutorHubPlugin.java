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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.hub.internal.FilteredHubListener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackgroundExecutorHubPlugin extends HubPlugin<Void, Void> {

    private static final String TAG = BackgroundExecutorHubPlugin.class.getSimpleName();

    private final Map<UUID, FilteredHubListener> listenersByUUID;
    private final Map<HubChannel, HashSet<FilteredHubListener>> listenersByHubChannel;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public BackgroundExecutorHubPlugin() {
        this.listenersByUUID = new ConcurrentHashMap<UUID, FilteredHubListener>();
        this.listenersByHubChannel = new ConcurrentHashMap<HubChannel, HashSet<FilteredHubListener>>();
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Dispatch a Hub message on the specified channel
     *
     * @param hubChannel The channel to send the message on
     * @param hubpayload The payload to send
     */
    @Override
    public void publish(@NonNull final HubChannel hubChannel, @NonNull final HubPayload hubpayload) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Set<FilteredHubListener> listenersOfHubChannel = listenersByHubChannel.get(hubChannel);
                        if (listenersOfHubChannel == null) {
                            return;
                        }

                        for (FilteredHubListener filteredHubListener: listenersOfHubChannel) {
                            if (filteredHubListener.getHubFilter() == null ||
                                filteredHubListener.getHubFilter().filter(hubpayload)) {
                                filteredHubListener.getHubListener().onEvent(hubpayload);
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Listen to Hub messages on a particular channel,
     *
     * @param hubChannel The channel to listen for messages on
     * @param listener   The callback to invoke with the received message
     * @return the token which serves as an identifier for the listener
     * registered. The token can be used with
     * {@link #unsubscribe(SubscriptionToken)}
     * to de-register the listener.
     */
    @Override
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel, @Nullable HubListener listener) {
        return subscribe(hubChannel, null, listener);
    }

    /**
     * Listen to Hub messages on a particular channel,
     *
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
    public SubscriptionToken subscribe(@NonNull HubChannel hubChannel, @Nullable HubPayloadFilter hubPayloadFilter, @Nullable HubListener listener) {
        final UUID listenerId = UUID.randomUUID();
        final FilteredHubListener filteredHubListener = new FilteredHubListener(hubChannel, listenerId, hubPayloadFilter, listener);

        listenersByUUID.put(listenerId, filteredHubListener);
        Set<FilteredHubListener> filteredHubListeners = listenersByHubChannel.get(hubChannel);
        if (filteredHubListeners == null) {
            filteredHubListeners = new HashSet<FilteredHubListener>();
        }
        filteredHubListeners.add(filteredHubListener);

        return new SubscriptionToken(listenerId);
    }

    /**
     * The registered listener can be removed from the Hub system by passing the
     * token received from {@link #subscribe(HubChannel, HubListener)} or
     * {@link #subscribe(HubChannel, HubPayloadFilter, HubListener)}.
     *
     * @param subscriptionToken the token which serves as an identifier for the listener
     *                         {@link HubListener} registered
     */
    @Override
    public void unsubscribe(@NonNull SubscriptionToken subscriptionToken) {
        listenersByUUID.remove(subscriptionToken.getUuid());
    }

    /**
     * @return the identifier that identifies
     * the plugin implementation
     */
    @Override
    public String getPluginKey() {
        return "DefaultHubCategoryPlugin";
    }

    /**
     * Configure the plugin with customized configuration object
     *
     * @param pluginConfiguration plugin-specific configuration
     * @throws PluginException when configuration for a plugin was not found
     */
    @Override
    public void configure(Void pluginConfiguration) throws PluginException {

    }

    /**
     * Returns escape hatch for plugin to enable lower-level client use-cases
     *
     * @return the client used by category plugin
     */
    @Override
    public Void getEscapeHatch() {
        return null;
    }
}
