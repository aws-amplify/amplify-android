/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect

/**
 * Configuration for [AmplifyConnectClient].
 *
 * Points the client at the Customer Profiles identify endpoint (an HTTP API
 * fronting the backend Lambda) and the region used to SigV4-sign guest
 * requests.
 *
 * Parse from `amplify_outputs.json` with [fromAmplifyOutputs], or construct
 * directly for testing.
 *
 * @param endpoint The base identify endpoint URL
 * @param region The AWS region for SigV4-signing guest requests
 */
data class ConnectClientConfiguration(
    val endpoint: String,
    val region: String
) {
    init {
        require(endpoint.isNotBlank()) { "endpoint must not be blank" }
        require(region.isNotBlank()) { "region must not be blank" }
    }

    companion object {
        /**
         * Parses configuration from the `custom.CustomerProfiles` section of a
         * decoded amplify_outputs map.
         *
         * Expects:
         * ```json
         * {
         *   "custom": {
         *     "CustomerProfiles": {
         *       "endpoint": "https://abc123.execute-api.us-east-1.amazonaws.com",
         *       "region": "us-east-1"
         *     }
         *   }
         * }
         * ```
         *
         * @throws ConnectConfigurationException if the section or either required
         *   field is missing
         */
        @JvmStatic
        fun fromAmplifyOutputs(amplifyOutputs: Map<String, Any?>): ConnectClientConfiguration {
            val custom = amplifyOutputs["custom"]
            val section = (custom as? Map<*, *>)?.get("CustomerProfiles")
            if (section !is Map<*, *>) {
                throw ConnectConfigurationException(
                    "Missing \"custom.CustomerProfiles\" section in amplify_outputs."
                )
            }
            val endpoint = section["endpoint"]
            val region = section["region"]
            if (endpoint !is String || endpoint.isBlank()) {
                throw ConnectConfigurationException(
                    "Missing \"custom.CustomerProfiles.endpoint\" in amplify_outputs."
                )
            }
            if (region !is String || region.isBlank()) {
                throw ConnectConfigurationException(
                    "Missing \"custom.CustomerProfiles.region\" in amplify_outputs."
                )
            }
            return ConnectClientConfiguration(
                endpoint = endpoint.trimEnd('/'),
                region = region
            )
        }
    }
}
