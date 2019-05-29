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

package com.amplifyframework.analytics;

import android.support.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.plugin.Category;
import com.amplifyframework.core.plugin.CategoryPlugin;

public class Analytics extends Amplify {

    private static Category category = Category.ANALYTICS;

    private static boolean enabled = true;

    private static final Object LOCK = new Object();

    /**
     * This will record the analyticsEvent and eventually submit to the
     * registered plugin.
     *
     * @param analyticsEvent the object that encapsulates the event information
     */
    public static void record(@NonNull final AnalyticsEvent analyticsEvent) throws AnalyticsException {
        if (enabled) {
            CategoryPlugin analyticsPlugin = Amplify.getPluginForCategory(category);
            if (analyticsPlugin instanceof AnalyticsPlugin) {
                ((AnalyticsPlugin) analyticsPlugin).record(analyticsEvent);
            } else {
                throw new AnalyticsException("Failed to record analyticsEvent. " +
                        "Please check if a valid storage plugin is registered.");
            }
        }
    }

    /**
     * This will record the analyticsEvent and eventually submit to the
     * registered plugin.
     *
     * @param analyticsEvent the object that encapsulates the event information
     * @param pluginKey Key that identifies the plugin
     */
    public static void record(@NonNull final AnalyticsEvent analyticsEvent,
                              @NonNull final String pluginKey) throws AnalyticsException {
        if (enabled) {
            CategoryPlugin analyticsPlugin = Amplify.getPlugin(pluginKey);
            if (analyticsPlugin instanceof AnalyticsPlugin) {
                ((AnalyticsPlugin) analyticsPlugin).record(analyticsEvent);
            } else {
                throw new AnalyticsException("Failed to record analyticsEvent. " +
                        "Please check if a valid storage plugin is registered.");
            }
        }
    }

    /**
     * Enable collecting and sending Analytics events.
     */
    public static void enable() {
        synchronized (LOCK) {
            enabled = true;
        }
    }

    /**
     * Disable collecting and sending Analytics events.
     */
    public static void disable() {
        synchronized (LOCK) {
            enabled = false;
        }
    }
}
