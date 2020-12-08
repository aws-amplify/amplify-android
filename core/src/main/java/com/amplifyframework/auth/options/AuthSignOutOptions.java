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

/**
 * Advanced options for signing out.
 */
public class AuthSignOutOptions {
    private final boolean globalSignOut;

    /**
     * Advanced options for signing up.
     * @param globalSignOut Sign out of all devices (do not use when signing out of a WebUI experience)
     */
    protected AuthSignOutOptions(
            boolean globalSignOut
    ) {
        this.globalSignOut = globalSignOut;
    }

    /**
     * Specifics if Amplify should invalidate credentials for all devices for this user or just the current one.
     * @return True if sign out should sign out all devices and False if it should only sign out locally
     */
    public boolean isGlobalSignOut() {
        return globalSignOut;
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
     * When overriding, be sure to include globalSignOut in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isGlobalSignOut()
        );
    }

    /**
     * When overriding, be sure to include globalSignOut in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthSignOutOptions authSignOutOptions = (AuthSignOutOptions) obj;
            return ObjectsCompat.equals(isGlobalSignOut(), authSignOutOptions.isGlobalSignOut());
        }
    }

    /**
     * When overriding, be sure to include globalSignOut in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignOutOptions{" +
                "globalSignOut=" + isGlobalSignOut() +
                '}';
    }

    /**
     * The builder for this class.
     * @param <T> The type of builder - used to support plugin extensions of this.
     */
    public abstract static class Builder<T extends Builder<T>> {
        private boolean globalSignOut = false;

        /**
         * Return the type of builder this is so that chaining can work correctly without implicit casting.
         * @return the type of builder this is
         */
        public abstract T getThis();

        /**
         * Specifics if Amplify should invalidate credentials for all devices for this user or just the current one.
         * @return True if sign out should sign out all devices and False if it should sign out locally
         */
        public boolean isGlobalSignOut() {
            return globalSignOut;
        }

        /**
         * Specifics if Amplify should invalidate credentials for all devices for this user or just the current one.
         * @param globalSignOut True if sign out should sign out all devices and False if it should sign out locally
         * @return The builder object
         */
        public T globalSignOut(boolean globalSignOut) {
            this.globalSignOut = globalSignOut;
            return getThis();
        }

        /**
         * Build an instance of AuthSignOutOptions (or one of its subclasses).
         * @return an instance of AuthSignOutOptions (or one of its subclasses)
         */
        @NonNull
        public AuthSignOutOptions build() {
            return new AuthSignOutOptions(globalSignOut);
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
