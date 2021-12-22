/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Reads an {@link AWSApiPluginConfiguration} from a JSON object.
 * TODO: add support for authorization types other than ApiKey.
 * Currently, ApiKey is the only acceptable value for AuthorizationType,
 * and when provided, must be accompanied by an ApiKey key and
 * associated key.
 */
final class AWSApiPluginConfigurationReader {
    private AWSApiPluginConfigurationReader() { /* no instances */ }

    /**
     * Reads an {@link AWSApiPluginConfiguration} from a JSON object.
     * @param configurationJson The top-level JSON config for the AWS API plugin
     * @return A strongly typed model of the configuration
     * @throws ApiException If the configuration json cannot be parsed
     */
    static AWSApiPluginConfiguration readFrom(JSONObject configurationJson)
            throws ApiException {

        if (configurationJson == null) {
            throw new ApiException(
                "Null configuration JSON provided to AWS API plugin.",
                "Check that the content of the AWS API Plugin section of the amplifyconfiguration.json file hasn't " +
                "been accidentally deleted."
            );
        }

        return parseConfigurationJson(configurationJson);
    }

    private static AWSApiPluginConfiguration parseConfigurationJson(JSONObject configurationJson)
            throws ApiException {

        final AWSApiPluginConfiguration.Builder configBuilder = AWSApiPluginConfiguration.builder();

        try {
            Iterator<String> apiSpecIterator = configurationJson.keys();
            while (apiSpecIterator.hasNext()) {
                final String apiName = apiSpecIterator.next();
                JSONObject apiSpec = configurationJson.getJSONObject(apiName);

                for (final String requiredKey : ConfigKey.requiredKeys()) {
                    if (!apiSpec.has(requiredKey)) {
                        throw new ApiException(
                                "Failed to parse configuration, missing required key: " + requiredKey,
                                AmplifyException.TODO_RECOVERY_SUGGESTION
                        );
                    }
                }

                final EndpointType endpointType =
                        EndpointType.from(apiSpec.getString(ConfigKey.ENDPOINT_TYPE.key()));
                final AuthorizationType authorizationType =
                        AuthorizationType.from(apiSpec.getString(ConfigKey.AUTHORIZATION_TYPE.key()));

                final ApiConfiguration.Builder apiConfigBuilder = ApiConfiguration.builder()
                        .endpointType(endpointType)
                        .endpoint(apiSpec.getString(ConfigKey.ENDPOINT.key()))
                        .region(apiSpec.getString(ConfigKey.REGION.key()))
                        .authorizationType(authorizationType);
                if (apiSpec.has(ConfigKey.API_KEY.key())) {
                    apiConfigBuilder.apiKey(apiSpec.getString(ConfigKey.API_KEY.key()));
                }
                configBuilder.addApi(apiName, apiConfigBuilder.build());
            }
        } catch (JSONException | ApiException exception) {
            throw new ApiException(
                   "Failed to parse configuration JSON for AWS API Plugin",
                   exception,
                   "Check amplifyconfiguration.json to make sure the AWS API configuration section hasn't been " +
                   "wrongly modified."
            );
        }

        return configBuilder.build();
    }

    /**
     * An enumeration of the various keys that we expect to see in
     * an API configuration json.
     */
    enum ConfigKey {
        ENDPOINT_TYPE("endpointType", Importance.REQUIRED),
        ENDPOINT("endpoint", Importance.REQUIRED),
        REGION("region", Importance.REQUIRED),
        AUTHORIZATION_TYPE("authorizationType", Importance.REQUIRED),
        API_KEY("apiKey", Importance.OPTIONAL);

        private final String key;
        private final Importance importance;

        ConfigKey(String key, Importance importance) {
            this.key = key;
            this.importance = importance;
        }

        String key() {
            return key;
        }

        boolean isRequired() {
            return Importance.REQUIRED.equals(importance);
        }

        static Set<String> requiredKeys() {
            final Set<String> requiredKeys = new HashSet<>();
            for (final ConfigKey configKey : ConfigKey.values()) {
                if (configKey.isRequired()) {
                    requiredKeys.add(configKey.key());
                }
            }
            return Collections.unmodifiableSet(requiredKeys);
        }

        static Set<String> optionalKeys() {
            final Set<String> optionalKeys = new HashSet<>();
            for (final ConfigKey configKey : ConfigKey.values()) {
                if (!configKey.isRequired()) {
                    optionalKeys.add(configKey.key());
                }
            }
            return Collections.unmodifiableSet(optionalKeys);
        }

        enum Importance {
            REQUIRED,
            OPTIONAL
        }
    }
}
