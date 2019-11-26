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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * API configuration details.
 */
final class ApiConfiguration {
    private final String endpoint;
    private final String region;
    private final AuthorizationType authorizationType;
    private final String apiKey;

    ApiConfiguration(Builder builder) {
        this.endpoint = builder.getEndpoint();
        this.region = builder.getRegion();
        this.authorizationType = builder.getAuthorizationType();
        this.apiKey = builder.getApiKey();
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
        private String endpoint;
        private String region;
        private AuthorizationType authorizationType;
        private String apiKey;

        Builder endpoint(@NonNull String endpoint) {
            Builder.this.endpoint = Objects.requireNonNull(endpoint);
            return Builder.this;
        }

        Builder region(@NonNull String region) {
            Builder.this.region = Objects.requireNonNull(region);
            return Builder.this;
        }

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

        ApiConfiguration build() {
            Objects.requireNonNull(Builder.this.endpoint);
            Objects.requireNonNull(Builder.this.region);
            Objects.requireNonNull(Builder.this.authorizationType);
            return new ApiConfiguration(Builder.this);
        }

        String getEndpoint() {
            return Builder.this.endpoint;
        }

        String getRegion() {
            return Builder.this.region;
        }

        AuthorizationType getAuthorizationType() {
            return Builder.this.authorizationType;
        }

        String getApiKey() {
            return Builder.this.apiKey;
        }
    }
}
