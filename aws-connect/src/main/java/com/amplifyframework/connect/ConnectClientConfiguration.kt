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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.core.configuration.AmplifyOutputsData

/**
 * Configuration for [AmplifyConnectClient].
 *
 * Points the client at the Customer Profiles identify endpoint (an HTTP API
 * fronting the backend Lambda) and the region used to SigV4-sign guest
 * requests.
 *
 * Parse from an [AmplifyOutputsData] with [fromAmplifyOutputs], or construct
 * directly for testing.
 *
 * @param endpoint The base identify endpoint URL
 * @param region The AWS region for SigV4-signing guest requests
 */
@ExperimentalAmplifyApi
data class ConnectClientConfiguration(
    val endpoint: String,
    val region: String
) {
    init {
        require(endpoint.isNotBlank()) { "endpoint must not be blank" }
        require(endpoint.startsWith("https://")) { "endpoint must use https" }
        require(region.isNotBlank()) { "region must not be blank" }
    }

    companion object {
        /**
         * Reads Connect configuration from the `notifications.amazon_connect`
         * section of a decoded [AmplifyOutputsData].
         *
         * @throws ConnectConfigurationException if the section is missing or the
         *   endpoint is not https
         */
        @JvmStatic
        fun fromAmplifyOutputs(outputs: AmplifyOutputsData): ConnectClientConfiguration {
            val connect = outputs.notifications?.amazonConnect
                ?: throw ConnectConfigurationException(
                    "Missing \"notifications.amazon_connect\" section in amplify_outputs."
                )
            if (!connect.endpoint.startsWith("https://")) {
                throw ConnectConfigurationException(
                    "\"notifications.amazon_connect.endpoint\" must use https."
                )
            }
            return ConnectClientConfiguration(
                endpoint = connect.endpoint.trimEnd('/'),
                region = connect.awsRegion
            )
        }
    }
}
