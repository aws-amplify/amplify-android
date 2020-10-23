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

package com.amplifyframework.api.aws.auth;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Utility class to easily generate a fake JWT token with desired header and payload.
 * This class is not equipped to generate and append a valid signature to the token.
 */
public final class FakeJWTToken {
    private final String token;

    private FakeJWTToken(String token) {
        this.token = token;
    }

    /**
     * Get the generated JWT token string.
     * @return the generated token
     */
    public String asString() {
        return token;
    }

    /**
     * Get the builder instance for constructing a token.
     * @return the builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder instance to specify the contents of a generated JWT token.
     */
    public static final class Builder {
        private static final String TOKEN_SEPARATOR = ".";
        private static final String DEFAULT_SIGNATURE = "FAKE-SIGNATURE";
        private JSONObject header;
        private JSONObject payload;
        private String signature;

        private Builder() {
            this.header = new JSONObject();
            this.payload = new JSONObject();
            this.signature = DEFAULT_SIGNATURE;
        }

        /**
         * Sets the header and return self for chaining.
         * @param header the token header json
         * @return this builder instance
         */
        @NonNull
        public Builder setHeader(@NonNull JSONObject header) {
            this.header = Objects.requireNonNull(header);
            return this;
        }

        /**
         * Sets the payload and return self for chaining.
         * @param payload the token payload json
         * @return this builder instance
         */
        @NonNull
        public Builder setPayload(@NonNull JSONObject payload) {
            this.payload = Objects.requireNonNull(payload);
            return this;
        }

        /**
         * Put a key-value pair to the header and return self for chaining.
         * If the key already exists, then update the value.
         * @param key key to add to header
         * @param value value that corresponds with the key
         * @return this builder instance
         */
        @NonNull
        public Builder putHeader(@NonNull String key, @NonNull String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            try {
                header.put(key, value);
            } catch (JSONException exception) {
                header.remove(key);
                return putHeader(key, value);
            }
            return this;
        }

        /**
         * Put a key-value pair to the payload and return self for chaining.
         * If the key already exists, then update the value.
         * @param key key to add to header
         * @param value value that corresponds with the key
         * @return this builder instance
         */
        @NonNull
        public Builder putPayload(@NonNull String key, @NonNull String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            try {
                payload.put(key, value);
            } catch (JSONException exception) {
                payload.remove(key);
                return putHeader(key, value);
            }
            return this;
        }

        /**
         * Sets the signature string and return self for chaining.
         * Defaults to "FAKE-SIGNATURE".
         * @param signature the signature for validation
         * @return this builder instance
         */
        @NonNull
        public Builder setSignature(@NonNull String signature) {
            this.signature = Objects.requireNonNull(signature);
            return this;
        }

        /**
         * Generate a fake JWT token with the contents of this builder.
         * @return a fake JWT token
         */
        @NonNull
        public FakeJWTToken build() {
            String token = base64Encode(header) +
                    TOKEN_SEPARATOR +
                    base64Encode(payload) +
                    TOKEN_SEPARATOR +
                    signature;
            return new FakeJWTToken(token);
        }

        private String base64Encode(JSONObject json) {
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(bytes);
        }
    }
}
