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

import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.configuration.IdentifyEntitiesConfiguration;
import com.amplifyframework.predictions.aws.configuration.InterpretTextConfiguration;
import com.amplifyframework.predictions.aws.configuration.TranscriptionConfiguration;
import com.amplifyframework.predictions.aws.configuration.TranslateTextConfiguration;

import com.amazonaws.regions.Region;
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
    private final TranscriptionConfiguration transcriptionConfiguration;
    private final IdentifyEntitiesConfiguration identifyEntitiesConfiguration;
    private final InterpretTextConfiguration interpretTextConfiguration;

    private AWSPredictionsPluginConfiguration(
            Region defaultRegion,
            TranslateTextConfiguration translateTextConfiguration,
            TranscriptionConfiguration transcriptionConfiguration,
            IdentifyEntitiesConfiguration identifyEntitiesConfiguration,
            InterpretTextConfiguration interpretTextConfiguration
    ) {
        this.defaultRegion = defaultRegion;
        this.defaultNetworkPolicy = NetworkPolicy.AUTO;
        this.translateTextConfiguration = translateTextConfiguration;
        this.transcriptionConfiguration = transcriptionConfiguration;
        this.identifyEntitiesConfiguration = identifyEntitiesConfiguration;
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
        final TranslateTextConfiguration translateTextConfiguration;
        final TranscriptionConfiguration transcriptionConfiguration;
        final InterpretTextConfiguration interpretConfiguration;
        final IdentifyEntitiesConfiguration identifyEntitiesConfiguration;

        // Required sections
        try {
            // Get default region
            String regionString = configurationJson.getString(ConfigKey.DEFAULT_REGION.key());
            defaultRegion = Region.getRegion(regionString);

            if (configurationJson.has(ConfigKey.CONVERT.key())) {
                JSONObject convertJson = configurationJson.getJSONObject(ConfigKey.CONVERT.key());
                translateTextConfiguration = TranslateTextConfiguration.fromJson(convertJson);
                transcriptionConfiguration = TranscriptionConfiguration.fromJson(convertJson);
            } else {
                translateTextConfiguration = null;
                transcriptionConfiguration = null;
            }

            if (configurationJson.has(ConfigKey.IDENTIFY.key())) {
                JSONObject identifyJson = configurationJson.getJSONObject(ConfigKey.IDENTIFY.key());
                identifyEntitiesConfiguration = IdentifyEntitiesConfiguration.fromJson(identifyJson);
            } else {
                identifyEntitiesConfiguration = null;
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
                transcriptionConfiguration,
                identifyEntitiesConfiguration,
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
     * @throws PredictionsException if not configured
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
     * Gets the configuration for transcription.
     * Null if not configured.
     * @return the configuration for transcription
     * @throws PredictionsException if not configured
     */
    @NonNull
    public TranscriptionConfiguration getTranscriptionConfiguration() throws PredictionsException {
        if (transcriptionConfiguration == null) {
            throw new PredictionsException(
                    "Transcription is not configured.",
                    "Verify that transcription is configured under " + ConfigKey.CONVERT.key()
            );
        }
        return transcriptionConfiguration;
    }

    /**
     * Gets the configuration for entities detection.
     * Null if not configured.
     * @return the configuration for entities detection
     * @throws PredictionsException if not configured
     */
    @NonNull
    public IdentifyEntitiesConfiguration getIdentifyEntitiesConfiguration() throws PredictionsException {
        if (identifyEntitiesConfiguration == null) {
            throw new PredictionsException(
                    "Entities detection is not configured.",
                    "Verify that identifyEntities is configured under " + ConfigKey.IDENTIFY.key()
            );
        }
        return identifyEntitiesConfiguration;
    }

    /**
     * Gets the configuration for text interpretation.
     * Null if not configured.
     * @return the configuration for text interpretation
     * @throws PredictionsException if not configured
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
        IDENTIFY("identify"),
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
