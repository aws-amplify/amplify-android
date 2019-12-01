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

import android.net.Uri;

import com.amplifyframework.api.ApiException;

import org.json.JSONException;
import org.json.JSONObject;

final class SubscriptionAuthorizationHeader {
    @SuppressWarnings("checkstyle:all") private SubscriptionAuthorizationHeader() {}

    /**
     * Return authorization json to be used for connection and subscription registration.
     */
    static JSONObject from(ApiConfiguration apiConfiguration) {
        final String host = Uri.parse(apiConfiguration.getEndpoint()).getHost();
        final String apiKey = apiConfiguration.getApiKey();
        try {
            return new JSONObject()
                .put("host", host)
                .put("x-amz-date", Iso8601Timestamp.now())
                .put("x-api-key", apiKey);
        } catch (JSONException jsonException) {
            throw new ApiException("Error constructing the authorization json for Api key. ", jsonException);
        }
    }
}
