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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An OkHttp3 interceptor which applies a User-Agent header to an outgoing request.
 */
final class UserAgentInterceptor implements Interceptor {
    private final UserAgentProvider userAgentProvider;

    /**
     * Constructs a UserAgentInterceptor.
     * @param userAgentProvider A Provider of a user-agent string
     */
    private UserAgentInterceptor(final UserAgentProvider userAgentProvider) {
        this.userAgentProvider = userAgentProvider;
    }

    /**
     * Creates a user agent interceptor using a user-agent string provider.
     * @param userAgentProvider Provider of user-agent string
     * @return A UserAgentInterceptor
     */
    static UserAgentInterceptor using(UserAgentProvider userAgentProvider) {
        return new UserAgentInterceptor(userAgentProvider);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgentProvider.getUserAgent())
            .build();
        return chain.proceed(requestWithUserAgent);
    }

    /**
     * A provider of a user-agent string.
     */
    interface UserAgentProvider {
        /**
         * Gets the User-Agent string.
         * @return User-Agent string
         */
        String getUserAgent();
    }
}
