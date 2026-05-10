/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.options.AuthSignOutOptions;

/**
 * Cognito extension of sign out options to add the platform specific fields.
 */
public final class AWSCognitoAuthSignOutOptions extends AuthSignOutOptions {
    private final String browserPackage;
    private final boolean signOutAllUsers;

    /**
     * Advanced options for signing out.
     * @param globalSignOut Sign out of all devices (do not use when signing out of a WebUI experience)
     * @param browserPackage Specify which browser package should be used for signing out of an account which was signed
     *                      into with a web UI experience (example value: "org.mozilla.firefox").
     *                      Defaults to the Chrome package if not specified.
     * @param signOutAllUsers Multi-user opt-out. When true (default), a no-userId sign out iterates every user in
     *                       AuthStateRepo. When false, a no-userId sign out targets only the active user.
     */
    protected AWSCognitoAuthSignOutOptions(boolean globalSignOut, String browserPackage, boolean signOutAllUsers) {
        super(globalSignOut);
        this.browserPackage = browserPackage;
        this.signOutAllUsers = signOutAllUsers;
    }

    /**
     * Optional browser package override to choose a browser app other than Chrome to launch web sign out.
     * @return optional browser package override to choose a browser app other than Chrome to launch web sign out.
     */
    @Nullable
    public String getBrowserPackage() {
        return browserPackage;
    }

    /**
     * Multi-user sign-out toggle. Defaults to {@code true}.
     * <p>
     * When {@code true}, calling {@code signOut(options, …)} (no explicit userId) iterates every user in the per-user
     * state repository and signs each one out. When {@code false}, the same call targets only the currently active
     * user. Has no effect on {@code signOut(userId, options, …)}, which always targets the supplied userId.
     *
     * @return whether sign out should iterate all users when no userId is supplied.
     */
    public boolean isSignOutAllUsers() {
        return signOutAllUsers;
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
        return ObjectsCompat.hash(isGlobalSignOut(), getBrowserPackage(), isSignOutAllUsers());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthSignOutOptions authSignOutOptions = (AWSCognitoAuthSignOutOptions) obj;
            return ObjectsCompat.equals(isGlobalSignOut(), authSignOutOptions.isGlobalSignOut()) &&
                    ObjectsCompat.equals(getBrowserPackage(), authSignOutOptions.getBrowserPackage()) &&
                    ObjectsCompat.equals(isSignOutAllUsers(), authSignOutOptions.isSignOutAllUsers());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignOutOptions{" +
                "isGlobalSignOut=" + isGlobalSignOut() +
                ", browserPackage=" + getBrowserPackage() +
                ", signOutAllUsers=" + isSignOutAllUsers() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private String browserPackage;
        private boolean signOutAllUsers = true;

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        @Override
        public CognitoBuilder getThis() {
            return this;
        }

        /**
         * This can optionally be set to specify which browser package should perform the sign out action
         * (e.g. "org.mozilla.firefox") in the case of an account which was signed in to from a web UI experience.
         * Defaults to the Chrome package if not set.
         *
         * @param browserPackage String specifying the browser package to perform the web sign out action.
         * @return the instance of the builder.
         */
        public CognitoBuilder browserPackage(@NonNull String browserPackage) {
            this.browserPackage = browserPackage;
            return this;
        }

        /**
         * Multi-user sign-out toggle. Defaults to {@code true}.
         *
         * @param signOutAllUsers when true, a no-userId sign out iterates every user; when false, only the active user.
         * @return the instance of the builder.
         */
        public CognitoBuilder signOutAllUsers(boolean signOutAllUsers) {
            this.signOutAllUsers = signOutAllUsers;
            return this;
        }

        /**
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthSignOutOptions with the values specified in the builder.
         */
        @NonNull
        public AWSCognitoAuthSignOutOptions build() {
            return new AWSCognitoAuthSignOutOptions(super.isGlobalSignOut(), browserPackage, signOutAllUsers);
        }
    }
}
