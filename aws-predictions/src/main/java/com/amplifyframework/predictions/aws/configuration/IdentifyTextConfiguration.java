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
import com.amplifyframework.predictions.models.TextFormatType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configures the behavior for identifying text.
 */
public final class IdentifyTextConfiguration {
    private static final String CONFIG_NAME = "identifyText";
    private final TextFormatType format;
    private final NetworkPolicy networkPolicy;

    private IdentifyTextConfiguration(
            TextFormatType format,
            NetworkPolicy networkPolicy
    ) {
        this.format = format;
        this.networkPolicy = networkPolicy;
    }

    /**
     * Construct an instance of {@link IdentifyTextConfiguration} from
     * plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration for text identification
     * @throws JSONException if identify configuration is malformed
     */
    @Nullable
    public static IdentifyTextConfiguration fromJson(@NonNull JSONObject configurationJson) throws JSONException {
        if (!configurationJson.has(CONFIG_NAME)) {
            return null;
        }

        JSONObject identifyLabelsJson = configurationJson.getJSONObject(CONFIG_NAME);
        String formatString = identifyLabelsJson.getString("format");
        String networkPolicyString = identifyLabelsJson.getString("defaultNetworkPolicy");

        final TextFormatType format = TextFormatType.valueOf(formatString);
        final NetworkPolicy networkPolicy = NetworkPolicy.fromKey(networkPolicyString);

        return new IdentifyTextConfiguration(format, networkPolicy);
    }

    /**
     * Gets the type of text identification to perform.
     * @return the type of text identification
     */
    @NonNull
    public TextFormatType getFormat() {
        return format;
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
