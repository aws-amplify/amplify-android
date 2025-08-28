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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Advanced options for signing in with a hosted web UI.
 */
public class AuthWebUISignInOptions {
    private final List<String> scopes;
    private final Boolean preferPrivateSession;

    /**
     * Advanced options for signing in with a hosted web UI.
     * @param scopes specify OAUTH scopes
     * @param preferPrivateSession specifying whether or not to launch web ui in an ephemeral CustomTab.
     *                             Default value is unset (null), which behaves the same as false.
     */
    protected AuthWebUISignInOptions(List<String> scopes, Boolean preferPrivateSession) {
        this.scopes = scopes;
        this.preferPrivateSession = preferPrivateSession;
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
     * Optional override to prefer launching in an Ephemeral CustomTab, if available.
     * @return optional override to prefer launching in an Ephemeral CustomTab, if available.
     * Default value is unset (null), which behaves the same as false.
     */
    @Nullable
    public Boolean getPreferPrivateSession() {
        return preferPrivateSession;
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
     * When overriding, be sure to include scopes in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getScopes(),
                getPreferPrivateSession()
        );
    }

    /**
     * When overriding, be sure to include scopes in the comparison.
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
                    ObjectsCompat.equals(getPreferPrivateSession(), authWebUISignInOptions.getPreferPrivateSession());
        }
    }

    /**
     * When overriding, be sure to include scopes in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthWebUISignInOptions{" +
                "scopes=" + getScopes() +
                ", preferPrivateSession=" + getPreferPrivateSession() +
                '}';
    }

    /**
     * The builder for this class.
     * @param <T> The type of builder - used to support plugin extensions of this.
     */
    public abstract static class Builder<T extends Builder<T>> {
        private List<String> scopes;
        private Boolean preferPrivateSession;

        /**
         * Initialize the builder object with fields initialized with empty collection objects.
         */
        public Builder() {
            this.scopes = new ArrayList<>();
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
         * Optional override to prefer launching in an Ephemeral CustomTab, if available.
         * @return optional override to prefer launching in an Ephemeral CustomTab, if available.
         * Default value is unset (null), which behaves the same as false.
         */
        @Nullable
        public Boolean getPreferPrivateSession() {
            return preferPrivateSession;
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
         * This can optionally be set to prefer launching in an Ephemeral CustomTab, if available.
         *
         * @param preferPrivateSession Boolean specifying whether or not to launch web ui in an
         * ephemeral CustomTab. Default value is unset (null), which behaves the same as false.
         * @return the instance of the builder.
         */
        public T preferPrivateSession(@NonNull Boolean preferPrivateSession) {
            this.preferPrivateSession = preferPrivateSession;
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
                    preferPrivateSession
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
