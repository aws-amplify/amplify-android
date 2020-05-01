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
public final class IdentifyEntitiesConfiguration {

    private static final int MAX_VALID_ENTITIES = 50;

    private final int maxEntities;
    private final String collectionId;
    private final boolean generalEntityDetection;
    private final boolean celebrityDetectionEnabled;
    private final NetworkPolicy networkPolicy;

    private IdentifyEntitiesConfiguration(
            int maxEntities,
            String collectionId,
            boolean generalEntityDetection,
            boolean celebrityDetectionEnabled,
            NetworkPolicy networkPolicy
    ) {
        this.maxEntities = maxEntities;
        this.collectionId = collectionId;
        this.generalEntityDetection = generalEntityDetection;
        this.celebrityDetectionEnabled = celebrityDetectionEnabled;
        this.networkPolicy = networkPolicy;
    }

    /**
     * Construct an instance of {@link IdentifyEntitiesConfiguration} from
     * plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration for entities detection
     * @throws JSONException if identify configuration is malformed
     */
    @Nullable
    public static IdentifyEntitiesConfiguration fromJson(@NonNull JSONObject configurationJson) throws JSONException {
        if (!configurationJson.has("identifyEntities")) {
            return null;
        }

        // Required fields
        JSONObject identifyEntitiesJson = configurationJson.getJSONObject("identifyEntities");
        String celebEnabledString = identifyEntitiesJson.getString("celebrityDetectionEnabled");
        String networkPolicyString = identifyEntitiesJson.getString("defaultNetworkPolicy");

        boolean celebEnabled = Boolean.parseBoolean(celebEnabledString);
        NetworkPolicy networkPolicy = NetworkPolicy.fromKey(networkPolicyString);

        // Optional fields
        int maxEntities;
        boolean isGeneralEntityDetection;
        String collectionId;
        try {
            String maxEntitiesString = identifyEntitiesJson.getString("maxEntities");
            collectionId = identifyEntitiesJson.getString("collectionId");
            maxEntities = Integer.parseInt(maxEntitiesString);
            isGeneralEntityDetection = maxEntities < 1 || maxEntities > MAX_VALID_ENTITIES;
        } catch (JSONException | IllegalArgumentException exception) {
            collectionId = "";
            maxEntities = 0;
            isGeneralEntityDetection = true;
        }

        return new IdentifyEntitiesConfiguration(maxEntities, collectionId,
                isGeneralEntityDetection, celebEnabled, networkPolicy);
    }

    /**
     * Gets the max number of entities to detect per image.
     * @return the max number of detected entities
     */
    public int getMaxEntities() {
        return maxEntities;
    }

    /**
     * Gets the collection ID to match entities against.
     * @return the collection ID to find matching entities in
     */
    @NonNull
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * Returns true if configured to detect entities. False if
     * scoped to detect matching entities from collection.
     * @return true if configured for general entity detection
     */
    public boolean isGeneralEntityDetection() {
        return generalEntityDetection;
    }

    /**
     * Returns true if configured to allow celebrity detection.
     * @return true if configured to allow celebrity detection
     */
    public boolean isCelebrityDetectionEnabled() {
        return celebrityDetectionEnabled;
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
