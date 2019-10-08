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
import android.support.annotation.NonNull;
import android.util.Log;

import com.amplifyframework.core.plugin.PluginException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultHubPlugin extends HubPlugin {

    private static final String TAG = DefaultHubPlugin.class.getSimpleName();

    private static Map<Integer, HubCallback> listeners =
            new ConcurrentHashMap<Integer, HubCallback>();

    private Context context;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public DefaultHubPlugin(@NonNull final Context context) {
        this.context = context;
    }

    @Override
    public void listen(HubChannel hubChannel, HubCallback callback) {
        listeners.put(hubChannel.hashCode(), callback);
    }

    @Override
    public void remove(HubChannel hubChannel, HubCallback callback) {
        listeners.remove(hubChannel.hashCode());
    }

    @Override
    public void dispatch(final HubChannel hubChannel, final HubPayload hubPayload) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    listeners.get(hubChannel.hashCode()).onHubEvent(hubPayload);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getLocalizedMessage());
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
        return TAG;
    }

    @Override
    public void configure(@NonNull Object pluginConfiguration) throws PluginException {

    }

    @Override
    public Object getEscapeHatch() {
        return null;
    }
}
