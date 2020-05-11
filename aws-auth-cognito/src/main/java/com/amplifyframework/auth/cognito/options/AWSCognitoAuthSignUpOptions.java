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

/**
 * Cognito extension of sign up options to add the platform specific fields.
 */
public final class AWSCognitoAuthSignUpOptions extends AuthSignUpOptions {
    private final Map<String, String> validationData;

    /**
     * Advanced options for signing in.
     * @param userAttributes Additional user attributes which should be associated with this user on registration
     * @param validationData A map of custom key/values to be sent as part of the sign up process
     */
    protected AWSCognitoAuthSignUpOptions(
            List<AuthUserAttribute> userAttributes,
            Map<String, String> validationData
    ) {
        super(userAttributes);
        this.validationData = validationData;
    }

    /**
     * Get a map of custom key/values to be sent as part of the sign up process.
     * @return a map of custom key/values to be sent as part of the sign up process
     */
    @NonNull
    public Map<String, String> getValidationData() {
        return validationData;
    }

    /**
     * Returns a builder for this object.
     * @return a builder for this object.
     */
    @NonNull
    public static CognitoBuilder builder() {
        return new CognitoBuilder();
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
            AWSCognitoAuthSignUpOptions authSignUpOptions = (AWSCognitoAuthSignUpOptions) obj;
            return ObjectsCompat.equals(getUserAttributes(), authSignUpOptions.getUserAttributes()) &&
                    ObjectsCompat.equals(getValidationData(), authSignUpOptions.getValidationData());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignUpOptions{" +
                "userAttributes=" + getUserAttributes() +
                ", validationData=" + getValidationData() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private Map<String, String> validationData;

        /**
         * Constructs the builder.
         */
        public CognitoBuilder() {
            super();
            this.validationData = new HashMap<>();
        }

        /**
         * Gets the type of builder to support proper flow with this being an extended class.
         * @return the type of builder to support proper flow with this being an extended class.
         */
        @Override
        public CognitoBuilder getThis() {
            return this;
        }

        /**
         * A map of custom data the user can send as part of the sign up process for validation.
         * @param validationData A map of custom data the user can send as part of the sign up process for validation.
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder validationData(@NonNull Map<String, String> validationData) {
            Objects.requireNonNull(validationData);
            this.validationData.clear();
            this.validationData.putAll(validationData);
            return getThis();
        }

        /**
         * Build the object.
         * @return a new instance of AWSCognitoAuthSignUpOptions.
         */
        @NonNull
        public AWSCognitoAuthSignUpOptions build() {
            return new AWSCognitoAuthSignUpOptions(
                    Immutable.of(super.getUserAttributes()),
                    Immutable.of(validationData));
        }
    }
}
