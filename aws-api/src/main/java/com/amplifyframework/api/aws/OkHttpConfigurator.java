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

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;

import okhttp3.OkHttpClient;

/**
 * An OkHttpConfigurator is a hook provided to a customer, enabling them to customize
 * the way an API client is setup while the AWS API plugin is being instantiated.
 *
 * This hook is for advanced use cases, such as where a user may want to append some of
 * their own request headers, or otherwise manipulate an outgoing request.
 *
 * See {@link AWSApiPlugin.Builder#configureClient(String, OkHttpConfigurator)}
 * for more details.
 */
@FunctionalInterface
public interface OkHttpConfigurator {
    /**
     * A customer can implement this hook to apply additional configurations
     * for a particular API. The user supplies an implementation of this function
     * when the AWSApiPlugin is being constructed:
     * <pre>
     *     AWSApiPlugin plugin = AWSApiPlugin.builder()
     *         .configureClient("someApi", okHttpBuilder -> {
     *             okHttpBuilder.connectTimeout(10, TimeUnit.SECONDS);
     *         })
     *         .build();
     * </pre>
     * The hook itself is applied later, when {@link Amplify#configure(Context)} is invoked.
     *
     * @param okHttpClientBuilder An {@link OkHttpClient.Builder} instance
     */
    void applyConfiguration(@NonNull OkHttpClient.Builder okHttpClientBuilder);
}
