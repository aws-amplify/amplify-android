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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * API configuration details.
 */
public final class ApiConfiguration {
    private final EndpointType endpointType;
    private final String endpoint;
    private final String region;
    private final AuthorizationType authorizationType;
    private final String apiKey;

    @SuppressLint("SyntheticAccessor")
    private ApiConfiguration(Builder builder) {
        this.endpointType = builder.endpointType;
        this.endpoint = builder.endpoint;
        this.region = builder.region;
        this.authorizationType = builder.authorizationType;
        this.apiKey = builder.apiKey;
    }

    EndpointType getEndpointType() {
        return this.endpointType;
    }

    String getEndpoint() {
        return this.endpoint;
    }

    String getRegion() {
        return this.region;
    }

    AuthorizationType getAuthorizationType() {
        return this.authorizationType;
    }

    String getApiKey() {
        return this.apiKey;
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private EndpointType endpointType;
        private String endpoint;
        private String region;
        private AuthorizationType authorizationType;
        private String apiKey;

        Builder endpointType(@NonNull EndpointType endpointType) {
            Builder.this.endpointType = Objects.requireNonNull(endpointType);
            return Builder.this;
        }

        Builder endpoint(@NonNull String endpoint) {
            Builder.this.endpoint = Objects.requireNonNull(endpoint);
            return Builder.this;
        }

        Builder region(@NonNull String region) {
            Builder.this.region = Objects.requireNonNull(region);
            return Builder.this;
        }

        @SuppressWarnings("unused")
        Builder authorizationType(@NonNull String string) {
            return Builder.this.authorizationType(AuthorizationType.from(string));
        }

        Builder authorizationType(@NonNull AuthorizationType authorizationType) {
            Builder.this.authorizationType = Objects.requireNonNull(authorizationType);
            return Builder.this;
        }

        Builder apiKey(@Nullable String apiKey) {
            Builder.this.apiKey = apiKey;
            return Builder.this;
        }

        @SuppressLint("SyntheticAccessor")
        ApiConfiguration build() {
            Objects.requireNonNull(Builder.this.endpoint);
            Objects.requireNonNull(Builder.this.region);
            Objects.requireNonNull(Builder.this.authorizationType);
            return new ApiConfiguration(Builder.this);
        }
    }
}
