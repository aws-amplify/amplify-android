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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configures the behavior for text interpretation.
 */
public final class TranscriptionConfiguration {
    private static final String CONFIG_NAME = "transcription";
    private final String language;
    private final NetworkPolicy networkPolicy;

    private TranscriptionConfiguration(
            String language,
            NetworkPolicy networkPolicy
    ) {
        this.language = language;
        this.networkPolicy = networkPolicy;
    }

    /**
     * Construct an instance of {@link TranscriptionConfiguration} from
     * plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration for speech to text transcription
     * @throws JSONException if transcription configuration is malformed
     */
    @Nullable
    public static TranscriptionConfiguration fromJson(@NonNull JSONObject configurationJson) throws JSONException {
        if (!configurationJson.has(CONFIG_NAME)) {
            return null;
        }

        JSONObject transcriptionJson = configurationJson.getJSONObject(CONFIG_NAME);
        String language = transcriptionJson.getString("language");
        String networkPolicyString = transcriptionJson.getString("defaultNetworkPolicy");

        final NetworkPolicy networkPolicy = NetworkPolicy.fromKey(networkPolicyString);

        return new TranscriptionConfiguration(language, networkPolicy);
    }

    /**
     * Gets the default language of the input speech.
     * @return the default audio language
     */
    @NonNull
    public String getLanguage() {
        return language;
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
