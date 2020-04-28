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

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.util.Immutable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuthSignUpOptions {
    private final List<AuthUserAttribute> userAttributes;

    /**
     * Advanced options for signing in.
     * @param userAttributes Additional user attributes which should be associated with this user on registration
     */
    protected AuthSignUpOptions(
            List<AuthUserAttribute> userAttributes
    ) {
        this.userAttributes = userAttributes;
    }

    /**
     * Get additional user attributes which should be associated with this user on registration.
     * @return additional user attributes which should be associated with this user on registration
     */
    @NonNull
    public List<AuthUserAttribute> getUserAttributes() {
        return userAttributes;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * When overriding, be sure to include the parent properties in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getUserAttributes()
        );
    }

    /**
     * When overriding, be sure to include the parent properties in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthSignUpOptions authSignUpOptions = (AuthSignUpOptions) obj;
            return ObjectsCompat.equals(getUserAttributes(), authSignUpOptions.getUserAttributes());
        }
    }

    /**
     * When overriding, be sure to include the parent properties in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignUpOptions{" +
                "userAttributes=" + userAttributes +
                '}';
    }

    public static final class Builder {
        private List<AuthUserAttribute> userAttributes;

        public Builder() {
            this.userAttributes = new ArrayList<>();
        }

        @NonNull
        public Builder userAttributes(@NonNull List<AuthUserAttribute> userAttributes) {
            Objects.requireNonNull(userAttributes);
            this.userAttributes.clear();
            this.userAttributes.addAll(userAttributes);
            return this;
        }

        @NonNull
        public AuthSignUpOptions build() {
            return new AuthSignUpOptions(Immutable.of(userAttributes));
        }
    }
}
