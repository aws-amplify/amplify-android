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
import com.amplifyframework.predictions.aws.configuration.AWSInterpretConfiguration;

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
    private final AWSInterpretConfiguration interpretConfiguration;

    private AWSPredictionsPluginConfiguration(
            Region defaultRegion,
            AWSInterpretConfiguration interpretConfiguration
    ) {
        this.defaultRegion = defaultRegion;
        this.defaultNetworkPolicy = NetworkPolicy.AUTO;
        this.interpretConfiguration = interpretConfiguration;
    }

    /**
     * Constructs an instance of {@link AWSPredictionsPluginConfiguration} from
     * the plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration object for AWS Predictions Plugin
     * @throws PredictionsException if configuration is missing or malformed
     */
    @NonNull
    public static AWSPredictionsPluginConfiguration fromJson(JSONObject configurationJson) throws PredictionsException {
        if (configurationJson == null) {
            throw new PredictionsException(
                    "Could not locate predictions configuration for AWS Predictions Plugin.",
                    "Verify that amplifyconfiguration.json contains a section for \"awsPredictionsPlugin\"."
            );
        }

        final Region defaultRegion;
        final AWSInterpretConfiguration interpretConfiguration;
        try {
            // Get default region
            String regionString = configurationJson.getString("defaultRegion");
            defaultRegion = Region.getRegion(regionString);

            // Get interpret configuration
            interpretConfiguration = AWSInterpretConfiguration.fromJson(configurationJson);

            return new AWSPredictionsPluginConfiguration(
                    defaultRegion,
                    interpretConfiguration
            );
        } catch (JSONException | IllegalArgumentException exception) {
            throw new PredictionsException(
                    "Issue encountered while parsing configuration JSON",
                    exception,
                    "Check the attached exception for more details."
            );
        }
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
     * Gets the configuration for text interpretation.
     * Null if not configured.
     * @return the configuration for text interpretation
     */
    @NonNull
    public AWSInterpretConfiguration getInterpretConfiguration() {
        return interpretConfiguration;
    }
}
