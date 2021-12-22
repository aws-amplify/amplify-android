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

package com.amplifyframework.auth.cognito.options;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.options.AuthConfirmSignUpOptions;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cognito extension of confirm sign up options to add the platform specific fields.
 */
public final class AWSCognitoAuthConfirmSignUpOptions extends AuthConfirmSignUpOptions {
    private final Map<String, String> clientMetadata;

    /**
     * Advanced options for confirming sign up.
     * @param clientMetadata Additional custom attributes to be sent to the service such as information about the client
     */
    protected AWSCognitoAuthConfirmSignUpOptions(
            Map<String, String> clientMetadata
    ) {
        this.clientMetadata = clientMetadata;
    }

    /**
     * Get custom attributes to be sent to the service such as information about the client.
     * @return custom attributes to be sent to the service such as information about the client
     */
    @NonNull
    public Map<String, String> getClientMetadata() {
        return clientMetadata;
    }

    /**
     * Get a builder object.
     * @return a builder object.
     */
    @NonNull
    public static CognitoBuilder builder() {
        return new CognitoBuilder();
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getClientMetadata()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthConfirmSignUpOptions authConfirmSignUpOptions = (AWSCognitoAuthConfirmSignUpOptions) obj;
            return ObjectsCompat.equals(getClientMetadata(), authConfirmSignUpOptions.getClientMetadata());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthConfirmSignUpOptions{" +
                "metadata=" + clientMetadata +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private final Map<String, String> clientMetadata;

        /**
         * Constructor for the builder.
         */
        public CognitoBuilder() {
            super();
            this.clientMetadata = new HashMap<>();
        }

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        @Override
        public CognitoBuilder getThis() {
            return this;
        }

        /**
         * Set the metadata field for the object being built.
         * @param clientMetadata Custom user metadata to be sent with the sign up request.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder clientMetadata(@NonNull Map<String, String> clientMetadata) {
            Objects.requireNonNull(clientMetadata);
            this.clientMetadata.clear();
            this.clientMetadata.putAll(clientMetadata);
            return getThis();
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthConfirmSignUpOptions with the values specified in the builder.
         */
        @NonNull
        public AWSCognitoAuthConfirmSignUpOptions build() {
            return new AWSCognitoAuthConfirmSignUpOptions(
                    Immutable.of(clientMetadata));
        }
    }
}
