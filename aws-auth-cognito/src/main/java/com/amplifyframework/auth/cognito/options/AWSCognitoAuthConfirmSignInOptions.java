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

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.options.AuthConfirmSignInOptions;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cognito extension of confirm sign in options to add the platform specific fields.
 */
public final class AWSCognitoAuthConfirmSignInOptions extends AuthConfirmSignInOptions {
    private final Map<String, String> metadata;
    private final List<AuthUserAttribute> userAttributes;

    /**
     * Advanced options for confirming sign in.
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     * @param userAttributes A list of additional user attributes which should be
     * associated with this user on confirmSignIn.
     */
    protected AWSCognitoAuthConfirmSignInOptions(
            Map<String, String> metadata,
            List<AuthUserAttribute> userAttributes
    ) {
        this.metadata = metadata;
        this.userAttributes = userAttributes;
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
     * Get additional user attributes which should be associated with this user on confirmSignIn.
     * @return additional user attributes which should be associated with this user on confirmSignIn
     */
    @NonNull
    public List<AuthUserAttribute> getUserAttributes() {
        return userAttributes;
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
                getMetadata(),
                getUserAttributes()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthConfirmSignInOptions authConfirmSignInOptions = (AWSCognitoAuthConfirmSignInOptions) obj;
            return ObjectsCompat.equals(getMetadata(), authConfirmSignInOptions.getMetadata()) &&
                   ObjectsCompat.equals(getUserAttributes(), authConfirmSignInOptions.getUserAttributes());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthConfirmSignInOptions{" +
                "userAttributes=" + getUserAttributes() +
                ", metadata=" + getMetadata() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private Map<String, String> metadata;
        private List<AuthUserAttribute> userAttributes;

        /**
         * Constructor for the builder.
         */
        public CognitoBuilder() {
            super();
            this.metadata = new HashMap<>();
            this.userAttributes = new ArrayList<>();
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
         * Set the userAttributes field for the object being built.
         * @param userAttributes A list of additional user attributes which should be
*        * associated with this user on confirmSignIn.
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder userAttributes(@NonNull List<AuthUserAttribute> userAttributes) {
            Objects.requireNonNull(userAttributes);
            this.userAttributes.clear();
            this.userAttributes.addAll(userAttributes);
            return getThis();
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthConfirmSignInOptions with the values specified in the builder.
         */
        @NonNull
        public AWSCognitoAuthConfirmSignInOptions build() {
            return new AWSCognitoAuthConfirmSignInOptions(
                Immutable.of(metadata),
                Immutable.of(userAttributes));
        }
    }
}
