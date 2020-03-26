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

package com.amplifyframework.logging;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * AWS's default implementation of the {@link LoggingCategoryBehavior},
 * which emits logs to Android's {@link Log} class.
 */
@SuppressWarnings("WeakerAccess")
final class AndroidLoggingPlugin extends LoggingPlugin<Void> {
    @NonNull
    @Override
    public Logger getDefaultLogger() {
        return forNamespaceAndThreshold(null, null);
    }

    @NonNull
    @Override
    public Logger forNamespaceAndThreshold(@Nullable String namespace, @Nullable LogLevel threshold) {
        if (namespace == null) {
            if (threshold == null) {
                return new AndroidLogger();
            } else {
                return new AndroidLogger(threshold);
            }
        } else {
            if (threshold == null) {
                return new AndroidLogger(namespace);
            } else {
                return new AndroidLogger(namespace, threshold);
            }
        }
    }

    @NonNull
    @Override
    public Logger forNamespace(@Nullable String namespace) {
        return forNamespaceAndThreshold(namespace, null);
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "AndroidLoggingPlugin";
    }

    @Override
    public void configure(
            @Nullable JSONObject pluginConfiguration,
            @NonNull Context context)
            throws LoggingException {
        // In the future, accept a log level configuration from JSON?
    }

    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }
}
