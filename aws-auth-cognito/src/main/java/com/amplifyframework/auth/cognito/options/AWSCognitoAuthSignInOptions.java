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

import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AWSCognitoAuthSignInOptions extends AuthSignInOptions {
    private final Map<String, String> metadata;

    /**
     * Advanced options for signing in.
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     */
    protected AWSCognitoAuthSignInOptions(
            Map<String, String> metadata
    ) {
        this.metadata = metadata;
    }

    /**
     * Get custom attributes to be sent to the service such as information about the client.
     * @return custom attributes to be sent to the service such as information about the client
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @NonNull
    public static CognitoBuilder builder() {
        return new CognitoBuilder();
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getMetadata()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthSignInOptions authSignInOptions = (AWSCognitoAuthSignInOptions) obj;
            return ObjectsCompat.equals(getMetadata(), authSignInOptions.getMetadata());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignInOptions{" +
                "metadata=" + metadata +
                '}';
    }

    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private Map<String, String> metadata;

        public CognitoBuilder() {
            super();
            this.metadata = new HashMap<>();
        }

        @Override
        public CognitoBuilder getThis() {
            return this;
        }

        @NonNull
        public CognitoBuilder metadata(@NonNull Map<String, String> metadata) {
            Objects.requireNonNull(metadata);
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return getThis();
        }

        @NonNull
        public AWSCognitoAuthSignInOptions build() {
            return new AWSCognitoAuthSignInOptions(
                    Immutable.of(metadata));
        }
    }
}
