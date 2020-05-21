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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Advanced options for signing in with a hosted web UI.
 */
public class AuthWebUISignInOptions {
    private final List<String> scopes;
    private final Map<String, String> signInQueryParameters;
    private final Map<String, String> signOutQueryParameters;
    private final Map<String, String> tokenQueryParameters;

    /**
     * Advanced options for signing in with a hosted web UI.
     * @param scopes specify OAUTH scopes
     * @param signInQueryParameters map of custom parameters to send associated with sign in process
     * @param signOutQueryParameters map of custom parameters to send associated with sign out process
     * @param tokenQueryParameters map of custom parameters to send associated with token
     */
    protected AuthWebUISignInOptions(
            List<String> scopes,
            Map<String, String> signInQueryParameters,
            Map<String, String> signOutQueryParameters,
            Map<String, String> tokenQueryParameters
    ) {
        this.scopes = scopes;
        this.signInQueryParameters = signInQueryParameters;
        this.signOutQueryParameters = signOutQueryParameters;
        this.tokenQueryParameters = tokenQueryParameters;
    }

    /**
     * Get the specified OAUTH scopes.
     * @return the specified OAUTH scopes.
     */
    @NonNull
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Get map of custom parameters to send associated with sign in process.
     * @return map of custom parameters to send associated with sign in process.
     */
    @NonNull
    public Map<String, String> getSignInQueryParameters() {
        return signInQueryParameters;
    }

    /**
     * Get map of custom parameters to send associated with sign out process.
     * @return map of custom parameters to send associated with sign out process
     */
    @NonNull
    public Map<String, String> getSignOutQueryParameters() {
        return signOutQueryParameters;
    }

    /**
     * Get map of custom parameters to send associated with token.
     * @return map of custom parameters to send associated with token
     */
    @NonNull
    public Map<String, String> getTokenQueryParameters() {
        return tokenQueryParameters;
    }

    /**
     * Get a builder to construct an instance of this object.
     * @return a builder to construct an instance of this object.
     */
    @NonNull
    public static Builder<?> builder() {
        return new CoreBuilder();
    }

    /**
     * When overriding, be sure to include scopes, signInQueryParameters, signOutQueryParameters, and
     * tokenQueryParameters in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getScopes(),
                getSignInQueryParameters(),
                getSignOutQueryParameters(),
                getTokenQueryParameters()
        );
    }

    /**
     * When overriding, be sure to include scopes, signInQueryParameters, signOutQueryParameters, and
     * tokenQueryParameters in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthWebUISignInOptions authWebUISignInOptions = (AuthWebUISignInOptions) obj;
            return ObjectsCompat.equals(getScopes(), authWebUISignInOptions.getScopes()) &&
                ObjectsCompat.equals(getSignInQueryParameters(), authWebUISignInOptions.getSignInQueryParameters()) &&
                ObjectsCompat.equals(getSignOutQueryParameters(), authWebUISignInOptions.getSignOutQueryParameters()) &&
                ObjectsCompat.equals(getTokenQueryParameters(), authWebUISignInOptions.getTokenQueryParameters());
        }
    }

    /**
     * When overriding, be sure to include scopes, signInQueryParameters, signOutQueryParameters, and
     * tokenQueryParameters in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthWebUISignInOptions{" +
                "scopes=" + getScopes() +
                ", signInQueryParameters=" + getSignInQueryParameters() +
                ", signOutQueryParameters=" + getSignOutQueryParameters() +
                ", tokenQueryParameters=" + getTokenQueryParameters() +
                '}';
    }

    /**
     * The builder for this class.
     * @param <T> The type of builder - used to support plugin extensions of this.
     */
    public abstract static class Builder<T extends Builder<T>> {
        private List<String> scopes;
        private Map<String, String> signInQueryParameters;
        private Map<String, String> signOutQueryParameters;
        private Map<String, String> tokenQueryParameters;

        /**
         * Initialize the builder object with fields initialized with empty collection objects.
         */
        public Builder() {
            this.scopes = new ArrayList<>();
            this.signInQueryParameters = new HashMap<>();
            this.signOutQueryParameters = new HashMap<>();
            this.tokenQueryParameters = new HashMap<>();
        }

        /**
         * Return the type of builder this is so that chaining can work correctly without implicit casting.
         * @return the type of builder this is
         */
        public abstract T getThis();

        /**
         * Specified OAUTH scopes.
         * @return specified OAUTH scopes
         */
        public List<String> getScopes() {
            return scopes;
        }

        /**
         * Map of custom parameters to send associated with sign in process.
         * @return map of custom parameters to send associated with sign in process
         */
        public Map<String, String> getSignInQueryParameters() {
            return signInQueryParameters;
        }

        /**
         * Map of custom parameters to send associated with sign out process.
         * @return map of custom parameters to send associated with sign out process
         */
        public Map<String, String> getSignOutQueryParameters() {
            return signOutQueryParameters;
        }

        /**
         * Map of custom parameters to send associated with token.
         * @return map of custom parameters to send associated with token
         */
        public Map<String, String> getTokenQueryParameters() {
            return tokenQueryParameters;
        }

        /**
         * Map of custom parameters to send associated with sign in process.
         * @param scopes specify OAUTH scopes
         * @return The type of Builder object being used.
         */
        @NonNull
        public T scopes(@NonNull List<String> scopes) {
            Objects.requireNonNull(scopes);
            this.scopes.clear();
            this.scopes.addAll(scopes);
            return getThis();
        }

        /**
         * Map of custom parameters to send associated with sign in process.
         * @param signInQueryParameters map of custom parameters to send associated with sign in process
         * @return The type of Builder object being used.
         */
        @NonNull
        public T signInQueryParameters(@NonNull Map<String, String> signInQueryParameters) {
            Objects.requireNonNull(signInQueryParameters);
            this.signInQueryParameters.clear();
            this.signInQueryParameters.putAll(signInQueryParameters);
            return getThis();
        }

        /**
         * Map of custom parameters to send associated with sign out process.
         * @param signOutQueryParameters map of custom parameters to send associated with sign out process
         * @return The type of Builder object being used.
         */
        @NonNull
        public T signOutQueryParameters(@NonNull Map<String, String> signOutQueryParameters) {
            Objects.requireNonNull(signOutQueryParameters);
            this.signOutQueryParameters.clear();
            this.signOutQueryParameters.putAll(signOutQueryParameters);
            return getThis();
        }

        /**
         * Map of custom parameters to send associated with token.
         * @param tokenQueryParameters map of custom parameters to send associated with token
         * @return The type of Builder object being used.
         */
        @NonNull
        public T tokenQueryParameters(@NonNull Map<String, String> tokenQueryParameters) {
            Objects.requireNonNull(tokenQueryParameters);
            this.tokenQueryParameters.clear();
            this.tokenQueryParameters.putAll(tokenQueryParameters);
            return getThis();
        }

        /**
         * Build an instance of AuthWebUISignInOptions (or one of its subclasses).
         * @return an instance of AuthWebUISignInOptions (or one of its subclasses)
         */
        @NonNull
        public AuthWebUISignInOptions build() {
            return new AuthWebUISignInOptions(
                    Immutable.of(scopes),
                    Immutable.of(signInQueryParameters),
                    Immutable.of(signOutQueryParameters),
                    Immutable.of(tokenQueryParameters)
            );
        }
    }

    /**
     * The specific implementation of builder for this as the parent class.
     */
    public static final class CoreBuilder extends Builder<CoreBuilder> {

        @Override
        public CoreBuilder getThis() {
            return this;
        }
    }
}
