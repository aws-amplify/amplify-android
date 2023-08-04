/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.options;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cognito extension of sign in options to add the platform specific fields.
 */
public final class AWSCognitoAuthSignInOptions extends AuthSignInOptions {
    private final Map<String, String> metadata;
    private final AuthFlowType authFlowType;

    /**
     * Advanced options for signing in.
     *
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     * @param authFlowType AuthFlowType to be used by signIn API
     */
    private AWSCognitoAuthSignInOptions(
            @NonNull Map<String, String> metadata,
            AuthFlowType authFlowType
    ) {
        this.metadata = metadata;
        this.authFlowType = authFlowType;
    }

    /**
     * Get custom attributes to be sent to the service such as information about the client.
     *
     * @return custom attributes to be sent to the service such as information about the client
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Get authFlowType to be sent to the service.
     *
     * @return authFlowType to be sent to the signIn api
     */
    @Nullable
    public AuthFlowType getAuthFlowType() {
        return authFlowType;
    }

    /**
     * Get a builder object.
     *
     * @return a builder object.
     */
    @NonNull
    public static CognitoBuilder builder() {
        return new CognitoBuilder();
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(getMetadata(), getAuthFlowType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthSignInOptions authSignInOptions = (AWSCognitoAuthSignInOptions) obj;
            return ObjectsCompat.equals(getMetadata(), authSignInOptions.getMetadata()) &&
                    ObjectsCompat.equals(getAuthFlowType(), authSignInOptions.getAuthFlowType());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignInOptions{" +
                "metadata=" + getMetadata() +
                ", authFlowType=" + getAuthFlowType() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private final Map<String, String> metadata;
        private AuthFlowType authFlowType;

        /**
         * Constructor for the builder.
         */
        public CognitoBuilder() {
            super();
            this.metadata = new HashMap<>();
        }

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         *
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        @Override
        public CognitoBuilder getThis() {
            return this;
        }

        /**
         * Set the metadata field for the object being built.
         *
         * @param metadata Custom user metadata to be sent with the sign in request.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder metadata(@NonNull Map<String, String> metadata) {
            Objects.requireNonNull(metadata);
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return getThis();
        }

        /**
         * Set the authFlowType for the object being built.
         *
         * @param authFlowType authFlowType to be sent to sign in request.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder authFlowType(@NonNull AuthFlowType authFlowType) {
            this.authFlowType = authFlowType;
            return getThis();
        }

        /**
         * Construct and return the object with the values set in the builder.
         *
         * @return a new instance of AWSCognitoAuthSignInOptions with the values specified in the builder.
         */
        @NonNull
        public AWSCognitoAuthSignInOptions build() {
            return new AWSCognitoAuthSignInOptions(Immutable.of(metadata), authFlowType);
        }
    }
}
