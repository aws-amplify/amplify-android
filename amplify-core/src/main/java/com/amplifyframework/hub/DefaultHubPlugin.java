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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.hub.internal.FilteredHubListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultHubPlugin implements HubPlugin {

    private static final String TAG = DefaultHubPlugin.class.getSimpleName();

    private Map<UUID, FilteredHubListener> listeners;

    private ExecutorService executorService;

    private final Handler mainHandler;

    public DefaultHubPlugin() {
        this.listeners = new ConcurrentHashMap<UUID, FilteredHubListener>();
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public UnsubscribeToken listen(@NonNull final HubChannel hubChannel,
                                   @Nullable final HubListener listener) {
        return listen(hubChannel, null, listener);
    }

    @Override
    public UnsubscribeToken listen(@NonNull final HubChannel hubChannel,
                                   @Nullable final HubFilter hubFilter,
                                   @Nullable final HubListener listener) {
        final UUID listenerId = UUID.randomUUID();
        listeners.put(listenerId, new FilteredHubListener(hubChannel, listenerId, hubFilter, listener));
        return new UnsubscribeToken(listenerId, hubChannel);
    }

    @Override
    public void removeListener(@NonNull final UnsubscribeToken unsubscribeToken) {
        listeners.remove(unsubscribeToken.uuid);
    }

    @Override
    public void dispatch(@NonNull final HubChannel hubChannel,
                         @NonNull final HubPayload hubPayload) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                // TODO: Optimize this loop by storing the listeners by channel
                // so we could save on the compute time.
                for (final FilteredHubListener listener: listeners.values()) {
                    if (hubChannel.equals(listener.getChannel())) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.getHubListener().onHubEvent(hubPayload);
                            }
                        });
                    }
                }
            }
        });
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
     * Configure the Plugin with the configuration passed.
     *
     * @param pluginConfiguration configuration for the plugin
     * @throws PluginException when configuration for a plugin was not found
     */
    @Override
    public void configure(@NonNull HubPluginConfiguration pluginConfiguration) throws PluginException {

    }

    /**
     * Configure the Plugin using the details in amplifyconfiguration.json
     *
     * @param context Android context required to read the contents of file
     * @throws PluginException when configuration for a plugin was not found
     */
    @Override
    public void configure(@NonNull Context context) throws PluginException {

    }

    /**
     * Reset the plugin to the state where it's not configured.
     */
    @Override
    public void reset() {

    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.HUB;
    }
}
