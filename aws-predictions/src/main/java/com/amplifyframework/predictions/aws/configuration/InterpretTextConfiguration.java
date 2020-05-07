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
public final class InterpretTextConfiguration {
    private static final String CONFIG_NAME = "interpretText";
    private final InterpretType type;
    private final NetworkPolicy networkPolicy;

    private InterpretTextConfiguration(
            InterpretType type,
            NetworkPolicy networkPolicy
    ) {
        this.type = type;
        this.networkPolicy = networkPolicy;
    }

    /**
     * Construct an instance of {@link InterpretTextConfiguration} from
     * plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration for text interpretation
     * @throws JSONException if interpret configuration is malformed
     */
    @Nullable
    public static InterpretTextConfiguration fromJson(@NonNull JSONObject configurationJson) throws JSONException {
        if (!configurationJson.has(CONFIG_NAME)) {
            return null;
        }

        JSONObject interpretTextJson = configurationJson.getJSONObject(CONFIG_NAME);
        String typeString = interpretTextJson.getString("type");
        String networkPolicyString = interpretTextJson.getString("defaultNetworkPolicy");

        final InterpretType type = InterpretType.valueOf(typeString);
        final NetworkPolicy networkPolicy = NetworkPolicy.fromKey(networkPolicyString);

        return new InterpretTextConfiguration(type, networkPolicy);
    }

    /**
     * Gets the type of text interpretation to perform.
     * @return the type of text interpretation
     */
    @NonNull
    public InterpretType getType() {
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

    /**
     * Different types of text interpretation to perform.
     */
    public enum InterpretType {
        /**
         * Determine dominant language from text.
         */
        LANGUAGE,

        /**
         * Detect entities from text.
         */
        ENTITIES,

        /**
         * Pick out key phrases from text.
         */
        KEY_PHRASES,

        /**
         * Determine predominant sentiment from text.
         */
        SENTIMENT,

        /**
         * Identify parts of speech from text.
         */
        SYNTAX,

        /**
         * Determine all of the above from text.
         */
        ALL
    }
}
