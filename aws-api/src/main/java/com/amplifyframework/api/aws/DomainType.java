/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type of domain specified in API endpoint.
 */
public enum DomainType {
    /**
     * Invalid or unsupported domain type.
     */
    INVALID,

    /**
     * Standard domain type composed of randomly generated 26 char subdomain.
     */
    STANDARD,

    /**
     * Custom domain defined by user. It must be "https" and have path "graphql" in URL.
     */
    CUSTOM;

    private static final String STANDARD_ENDPOINT_REGEX =
            "^https:\\/\\/\\w{26}\\.appsync\\-api\\.\\w{2}(?:(?:\\-\\w{2,})+)\\-\\d\\.amazonaws.com\\/graphql$";

    /**
     * Get Domain type based on defined endpoint.
     * @param endpoint Endpoint defined in config.
     * @return {@link DomainType} based on supplied endpoint.
     */
    static DomainType from(String endpoint) {
        if (!(endpoint.startsWith("https://") && endpoint.endsWith("/graphql"))) {
            return INVALID;
        }

        if (isRegexMatch(endpoint, STANDARD_ENDPOINT_REGEX)) {
            return STANDARD;
        }

        return CUSTOM;
    }

    private static boolean isRegexMatch(String endpoint, String regex) {
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(endpoint);

        return matcher.matches();
    }
}
