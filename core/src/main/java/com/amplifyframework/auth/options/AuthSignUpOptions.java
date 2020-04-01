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

package com.amplifyframework.auth.options;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AuthSignUpOptions {
    private final Map<String, String> userAttributes;
    private final Map<String, String> validationData;

    /**
     * Advanced options for signing in.
     * @param userAttributes Additional user attributes which should be associated with this user on registration
     * @param validationData A map of custom key/values to be sent as part of the sign up process
     */
    private AuthSignUpOptions(
            Map<String, String> userAttributes,
            Map<String, String> validationData
    ) {
        this.userAttributes = userAttributes;
        this.validationData = validationData;
    }

    /**
     * Get additional user attributes which should be associated with this user on registration.
     * @return additional user attributes which should be associated with this user on registration
     */
    @NonNull
    public Map<String, String> getUserAttributes() {
        return userAttributes;
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
                getUserAttributes(),
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
            AuthSignUpOptions authSignUpOptions = (AuthSignUpOptions) obj;
            return ObjectsCompat.equals(getValidationData(), authSignUpOptions.getValidationData()) &&
                    ObjectsCompat.equals(getUserAttributes(), authSignUpOptions.getUserAttributes());
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("AuthSignUpOptions { ")
                .append("validationData: ")
                .append(getValidationData())
                .append(", userAttributes: ")
                .append(getUserAttributes())
                .append(" }")
                .toString();
    }

    public static final class Builder {
        private Map<String, String> userAttributes;
        private Map<String, String> validationData;

        public Builder() {
            this.userAttributes = new HashMap<>();
            this.validationData = new HashMap<>();
        }

        @NonNull
        public Builder userAttributes(@NonNull Map<String, String> userAttributes) {
            Objects.requireNonNull(userAttributes);
            this.userAttributes.clear();
            this.userAttributes.putAll(userAttributes);
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
        public AuthSignUpOptions build() {
            return new AuthSignUpOptions(Immutable.of(userAttributes), Immutable.of(validationData));
        }
    }
}
