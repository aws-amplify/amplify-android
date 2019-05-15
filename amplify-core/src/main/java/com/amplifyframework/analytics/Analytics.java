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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.provider.Category;
import com.amplifyframework.core.provider.Provider;

import java.util.Collection;


public class Analytics extends Amplify {

    private static Category category = Category.ANALYTICS;

    private static boolean enabled = true;

    private static final Object LOCK = new Object();

    /**
     * This will record the analyticsEvent and eventually submit to the
     * default provider.
     *
     * @param analyticsEvent the object that encapsulates the event information
     */
    public static void record(AnalyticsEvent analyticsEvent) throws AnalyticsException {
        if (enabled) {
            Collection<Provider> analyticsProviders = getProvidersForCategory(category);
            for (Provider provider : analyticsProviders) {
                if (provider != null && provider instanceof AnalyticsProvider) {
                    ((AnalyticsProvider) provider).record(analyticsEvent);
                } else {
                    throw new AnalyticsException("Failed to record analyticsEvent. " +
                            "Please check if a valid storage provider is registered.");
                }
            }
        }
    }

    /**
     *
     * @param analyticsEvent the object that encapsulates the event information
     * @param providerClass AnalyticProvider class
     */
    public static void record(final AnalyticsEvent analyticsEvent,
                              final Class<? extends Provider> providerClass) throws AnalyticsException {
        if (enabled) {
            Provider provider = Amplify.getProvider(providerClass);
            if (provider != null && provider instanceof AnalyticsProvider) {
                ((AnalyticsProvider) provider).record(analyticsEvent);
            } else {
                throw new AnalyticsException("Failed to record analyticsEvent. " +
                        "Please check if a valid storage provider is registered.");
            }
        }
    }

    public static void enable() {
        synchronized (LOCK) {
            enabled = true;
        }
    }

    public static void disable() {
        synchronized (LOCK) {
            enabled = false;
        }
    }
}
