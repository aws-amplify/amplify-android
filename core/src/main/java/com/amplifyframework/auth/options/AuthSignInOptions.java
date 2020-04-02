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

public final class AuthSignInOptions {
    private final Map<String, String> validationData;

    /**
     * Advanced options for signing in.
     * @param validationData A map of custom key/values to be sent as part of the sign in process
     */
    private AuthSignInOptions(Map<String, String> validationData) {
        this.validationData = validationData;
    }

    /**
     * Get a map of custom key/values to be sent as part of the sign in process.
     * @return a map of custom key/values to be sent as part of the sign in process
     */
    @NonNull
    public Map<String, String> getValidationData() {
        return validationData;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
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
            AuthSignInOptions authSignInOptions = (AuthSignInOptions) obj;
            return ObjectsCompat.equals(getValidationData(), authSignInOptions.getValidationData());
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("AuthSignInOptions { ")
                .append("validationData: ")
                .append(getValidationData())
                .append(" }")
                .toString();
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Map<String, String> validationData;

        public Builder() {
            this.validationData = new HashMap<>();
        }

        @NonNull
        public Builder validationData(@NonNull Map<String, String> validationData) {
            Objects.requireNonNull(validationData);
            this.validationData.clear();
            this.validationData.putAll(validationData);
            return this;
        }

        @NonNull
        public AuthSignInOptions build() {
            return new AuthSignInOptions(Immutable.of(validationData));
        }
    }
}
