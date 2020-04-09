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

package com.amplifyframework.predictions.aws;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.configuration.InterpretTextConfiguration;

import com.amazonaws.regions.Region;
import com.amplifyframework.predictions.aws.configuration.TranslateTextConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration options for the {@link AWSPredictionsPlugin}.
 * Contains settings for different types of predictions operations
 * as well as their network policy.
 */
public final class AWSPredictionsPluginConfiguration {
    private final Region defaultRegion;
    private final NetworkPolicy defaultNetworkPolicy;
    private final TranslateTextConfiguration translateTextConfiguration;
    private final InterpretTextConfiguration interpretTextConfiguration;

    private AWSPredictionsPluginConfiguration(
            Region defaultRegion,
            TranslateTextConfiguration translateTextConfiguration,
            InterpretTextConfiguration interpretTextConfiguration
    ) {
        this.defaultRegion = defaultRegion;
        this.defaultNetworkPolicy = NetworkPolicy.AUTO;
        this.translateTextConfiguration = translateTextConfiguration;
        this.interpretTextConfiguration = interpretTextConfiguration;
    }

    /**
     * Constructs an instance of {@link AWSPredictionsPluginConfiguration} from
     * the plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration object for AWS Predictions Plugin
     * @throws PredictionsException if configuration is missing or malformed
     */
    @NonNull
    static AWSPredictionsPluginConfiguration fromJson(JSONObject configurationJson) throws PredictionsException {
        if (configurationJson == null) {
            throw new PredictionsException(
                "Could not locate predictions configuration for AWS Predictions Plugin.",
                "Verify that amplifyconfiguration.json contains a section for \"awsPredictionsPlugin\"."
            );
        }

        final Region defaultRegion;
        final InterpretTextConfiguration interpretConfiguration;
        final TranslateTextConfiguration translateTextConfiguration;

        // Required sections
        try {
            // Get default region
            String regionString = configurationJson.getString(ConfigKey.DEFAULT_REGION.key());
            defaultRegion = Region.getRegion(regionString);

            if (configurationJson.has(ConfigKey.CONVERT.key())) {
                JSONObject convertJson = configurationJson.getJSONObject(ConfigKey.CONVERT.key());
                translateTextConfiguration = TranslateTextConfiguration.fromJson(convertJson);
            } else {
                translateTextConfiguration = null;
            }

            if (configurationJson.has(ConfigKey.INTERPRET.key())) {
                JSONObject interpretJson = configurationJson.getJSONObject(ConfigKey.INTERPRET.key());
                interpretConfiguration = InterpretTextConfiguration.fromJson(interpretJson);
            } else {
                interpretConfiguration = null;
            }

        } catch (JSONException | IllegalArgumentException exception) {
            throw new PredictionsException(
                    "Issue encountered while parsing configuration JSON",
                    exception,
                    "Check the attached exception for more details."
            );
        }

        return new AWSPredictionsPluginConfiguration(
                defaultRegion,
                translateTextConfiguration,
                interpretConfiguration
        );
    }

    /**
     * Gets the plugin-level default for the AWS endpoint region.
     * @return the default AWS endpoint region
     */
    @NonNull
    public Region getDefaultRegion() {
        return defaultRegion;
    }

    /**
     * Gets the plugin-level default for the network policy.
     * @return the default network policy
     */
    @NonNull
    public NetworkPolicy getDefaultNetworkPolicy() {
        return defaultNetworkPolicy;
    }

    /**
     * Gets the configuration for text translation.
     * Null if not configured.
     * @return the configuration for text translation
     */
    @NonNull
    public TranslateTextConfiguration getTranslateTextConfiguration() throws PredictionsException {
        if (translateTextConfiguration == null) {
            throw new PredictionsException(
                    "Text translation is not configured.",
                    "Verify that translateText is configured under " + ConfigKey.CONVERT.key()
            );
        }
        return translateTextConfiguration;
    }

    /**
     * Gets the configuration for text interpretation.
     * Null if not configured.
     * @return the configuration for text interpretation
     */
    @NonNull
    public InterpretTextConfiguration getInterpretTextConfiguration() throws PredictionsException {
        if (interpretTextConfiguration == null) {
            throw new PredictionsException(
                    "Text interpretation is not configured.",
                    "Verify that interpretText is configured under " + ConfigKey.INTERPRET.key()
            );
        }
        return interpretTextConfiguration;
    }

    /**
     * An enumeration of the various keys that we expect to see in
     * AWS Predictions configuration json.
     */
    enum ConfigKey {
        DEFAULT_REGION("defaultRegion"),
        CONVERT("convert"),
        INTERPRET("interpret");

        private final String key;

        ConfigKey(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }
    }
}
