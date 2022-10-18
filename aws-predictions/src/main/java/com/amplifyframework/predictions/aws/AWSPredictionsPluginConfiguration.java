/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.predictions.aws.configuration.IdentifyLabelsConfiguration;
import com.amplifyframework.predictions.aws.configuration.IdentifyTextConfiguration;
import com.amplifyframework.predictions.aws.configuration.InterpretTextConfiguration;
import com.amplifyframework.predictions.aws.configuration.SpeechGeneratorConfiguration;
import com.amplifyframework.predictions.aws.configuration.TranslateTextConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration options for the {@link AWSPredictionsPlugin}.
 * Contains settings for different types of predictions operations
 * as well as their network policy.
 */
public final class AWSPredictionsPluginConfiguration {
    private final String defaultRegion;
    private final NetworkPolicy defaultNetworkPolicy;
    private final SpeechGeneratorConfiguration speechGeneratorConfiguration;
    private final TranslateTextConfiguration translateTextConfiguration;
    private final IdentifyLabelsConfiguration identifyLabelsConfiguration;
    private final IdentifyEntitiesConfiguration identifyEntitiesConfiguration;
    private final IdentifyTextConfiguration identifyTextConfiguration;
    private final InterpretTextConfiguration interpretTextConfiguration;

    private AWSPredictionsPluginConfiguration(
            String defaultRegion,
            SpeechGeneratorConfiguration speechGeneratorConfiguration,
            TranslateTextConfiguration translateTextConfiguration,
            IdentifyLabelsConfiguration identifyLabelsConfiguration,
            IdentifyEntitiesConfiguration identifyEntitiesConfiguration,
            IdentifyTextConfiguration identifyTextConfiguration,
            InterpretTextConfiguration interpretTextConfiguration
    ) {
        this.defaultRegion = defaultRegion;
        this.defaultNetworkPolicy = NetworkPolicy.AUTO;
        this.speechGeneratorConfiguration = speechGeneratorConfiguration;
        this.translateTextConfiguration = translateTextConfiguration;
        this.identifyLabelsConfiguration = identifyLabelsConfiguration;
        this.identifyEntitiesConfiguration = identifyEntitiesConfiguration;
        this.identifyTextConfiguration = identifyTextConfiguration;
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

        final String defaultRegion;
        final SpeechGeneratorConfiguration speechGeneratorConfiguration;
        final TranslateTextConfiguration translateTextConfiguration;
        final IdentifyLabelsConfiguration identifyLabelsConfiguration;
        final IdentifyEntitiesConfiguration identifyEntitiesConfiguration;
        final IdentifyTextConfiguration identifyTextConfiguration;
        final InterpretTextConfiguration interpretConfiguration;

        try {
            // Get default region
            defaultRegion = configurationJson.getString(ConfigKey.DEFAULT_REGION.key());

            if (configurationJson.has(ConfigKey.CONVERT.key())) {
                JSONObject convertJson = configurationJson.getJSONObject(ConfigKey.CONVERT.key());
                speechGeneratorConfiguration = SpeechGeneratorConfiguration.fromJson(convertJson);
                translateTextConfiguration = TranslateTextConfiguration.fromJson(convertJson);
            } else {
                speechGeneratorConfiguration = null;
                translateTextConfiguration = null;
            }

            if (configurationJson.has(ConfigKey.IDENTIFY.key())) {
                JSONObject identifyJson = configurationJson.getJSONObject(ConfigKey.IDENTIFY.key());
                identifyLabelsConfiguration = IdentifyLabelsConfiguration.fromJson(identifyJson);
                identifyEntitiesConfiguration = IdentifyEntitiesConfiguration.fromJson(identifyJson);
                identifyTextConfiguration = IdentifyTextConfiguration.fromJson(identifyJson);
            } else {
                identifyLabelsConfiguration = null;
                identifyEntitiesConfiguration = null;
                identifyTextConfiguration = null;
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
                speechGeneratorConfiguration,
                translateTextConfiguration,
                identifyLabelsConfiguration,
                identifyEntitiesConfiguration,
                identifyTextConfiguration,
                interpretConfiguration
        );
    }

    /**
     * Gets the plugin-level default for the AWS endpoint region.
     * @return the default AWS endpoint region
     */
    @NonNull
    public String getDefaultRegion() {
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
     * Gets the configuration for speech generation.
     * Null if not configured.
     * @return the configuration for speech generation
     * @throws PredictionsException if not configured
     */
    @NonNull
    public SpeechGeneratorConfiguration getSpeechGeneratorConfiguration() throws PredictionsException {
        if (speechGeneratorConfiguration == null) {
            throw new PredictionsException(
                    "Speech generation is not configured.",
                    "Verify that speechGenerator is configured under " + ConfigKey.CONVERT.key()
            );
        }
        return speechGeneratorConfiguration;
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
     * Gets the configuration for labels detection.
     * Null if not configured.
     * @return the configuration for labels detection
     * @throws PredictionsException if not configured
     */
    @NonNull
    public IdentifyLabelsConfiguration getIdentifyLabelsConfiguration() throws PredictionsException {
        if (identifyLabelsConfiguration == null) {
            throw new PredictionsException(
                    "Labels detection is not configured.",
                    "Verify that identifyLabels is configured under " + ConfigKey.IDENTIFY.key()
            );
        }
        return identifyLabelsConfiguration;
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
     * Gets the configuration for text detection.
     * Null if not configured.
     * @return the configuration for text detection
     * @throws PredictionsException if not configured
     */
    @NonNull
    public IdentifyTextConfiguration getIdentifyTextConfiguration() throws PredictionsException {
        if (identifyTextConfiguration == null) {
            throw new PredictionsException(
                    "Text detection is not configured.",
                    "Verify that identifyText is configured under " + ConfigKey.IDENTIFY.key()
            );
        }
        return identifyTextConfiguration;
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
