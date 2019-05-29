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

import com.amplifyframework.core.exception.NoSuchPluginException;

public class AnalyticsCategory implements AnalyticsCategoryClientBehavior {
    public static AnalyticsCategoryPlugin getProvider(@NonNull String key)
            throws NoSuchPluginException {
        return null;
    }

    public static void removeProvider(@NonNull String key)
            throws NoSuchPluginException {

    }

    public static void disable() {

    }

    public static void enable() {

    }

    public static void recordEvent(String name) {

    }

    public static void recordEvent(AnalyticsEvent event) {

    }

    public static void updateProfile(AnalyticsProfile profile) {

    }
}
