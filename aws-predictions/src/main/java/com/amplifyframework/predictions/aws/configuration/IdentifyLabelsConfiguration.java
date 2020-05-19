/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.aws.NetworkPolicy;
import com.amplifyframework.predictions.models.LabelType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configures the behavior for identifying labels.
 */
public final class IdentifyLabelsConfiguration {
    private static final String CONFIG_NAME = "identifyLabels";
    private final LabelType type;
    private final NetworkPolicy networkPolicy;

    private IdentifyLabelsConfiguration(
            LabelType type,
            NetworkPolicy networkPolicy
    ) {
        this.type = type;
        this.networkPolicy = networkPolicy;
    }

    /**
     * Construct an instance of {@link IdentifyLabelsConfiguration} from
     * plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration for label identification
     * @throws JSONException if identify configuration is malformed
     */
    @Nullable
    public static IdentifyLabelsConfiguration fromJson(@NonNull JSONObject configurationJson) throws JSONException {
        if (!configurationJson.has(CONFIG_NAME)) {
            return null;
        }

        JSONObject identifyLabelsJson = configurationJson.getJSONObject(CONFIG_NAME);
        String typeString = identifyLabelsJson.getString("type");
        String networkPolicyString = identifyLabelsJson.getString("defaultNetworkPolicy");

        final LabelType type = LabelType.valueOf(typeString);
        final NetworkPolicy networkPolicy = NetworkPolicy.fromKey(networkPolicyString);

        return new IdentifyLabelsConfiguration(type, networkPolicy);
    }

    /**
     * Gets the type of label identification to perform.
     * @return the type of label identification
     */
    @NonNull
    public LabelType getType() {
        return type;
    }

    /**
     * Gets the type of network policy for resource access.
     * @return the network policy type
     */
    @NonNull
    public NetworkPolicy getNetworkPolicy() {
        return networkPolicy;
    }
}
