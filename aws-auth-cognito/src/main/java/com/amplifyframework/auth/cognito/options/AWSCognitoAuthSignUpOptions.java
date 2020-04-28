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

package com.amplifyframework.auth.cognito.options;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AWSCognitoAuthSignUpOptions extends AuthSignUpOptions {
    private final Map<String, String> metadata;
    private final Map<String, String> validationData;

    /**
     * Advanced options for signing in.
     * @param userAttributes Additional user attributes which should be associated with this user on registration
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     * @param validationData A map of custom key/values to be sent as part of the sign up process
     */
    protected AWSCognitoAuthSignUpOptions(
            List<AuthUserAttribute> userAttributes,
            Map<String, String> metadata,
            Map<String, String> validationData
    ) {
        super(userAttributes);
        this.metadata = metadata;
        this.validationData = validationData;
    }

    /**
     * Get custom attributes to be sent to the service such as information about the client.
     * @return custom attributes to be sent to the service such as information about the client
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Get a map of custom key/values to be sent as part of the sign up process.
     * @return a map of custom key/values to be sent as part of the sign up process
     */
    @NonNull
    public Map<String, String> getValidationData() {
        return validationData;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getMetadata(),
                getValidationData()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthSignUpOptions authSignUpOptions = (AWSCognitoAuthSignUpOptions) obj;
            return ObjectsCompat.equals(getValidationData(), authSignUpOptions.getValidationData()) &&
                    ObjectsCompat.equals(getMetadata(), authSignUpOptions.getMetadata());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignUpOptions{" +
                "metadata=" + metadata +
                ", validationData=" + validationData +
                '}';
    }

    public static final class Builder extends AuthSignUpOptions.Builder<Builder> {
        private Map<String, String> metadata;
        private Map<String, String> validationData;

        public Builder() {
            super();
            this.metadata = new HashMap<>();
            this.validationData = new HashMap<>();
        }

        @NonNull
        public Builder metadata(@NonNull Map<String, String> metadata) {
            Objects.requireNonNull(metadata);
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return this;
        }

        @NonNull
        public Builder validationData(@NonNull Map<String, String> validationData) {
            Objects.requireNonNull(validationData);
            this.validationData.clear();
            this.validationData.putAll(validationData);
            return this;
        }

        @NonNull
        public AWSCognitoAuthSignUpOptions build() {
            return new AWSCognitoAuthSignUpOptions(
                    Immutable.of(super.userAttributes),
                    Immutable.of(metadata),
                    Immutable.of(validationData));
        }
    }
}
