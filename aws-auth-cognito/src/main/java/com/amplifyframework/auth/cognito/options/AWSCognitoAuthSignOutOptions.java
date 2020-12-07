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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.options.AuthSignOutOptions;

/**
 * Cognito extension of sign out options to add the platform specific fields.
 */
public final class AWSCognitoAuthSignOutOptions extends AuthSignOutOptions {
    private final String browserPackage;

    /**
     * Advanced options for signing out.
     * @param globalSignOut Sign out of all devices (do not use when signing out of a WebUI experience)
     * @param browserPackage Specify which browser package should be used for signing out of an account which was signed
     *                      into with a web UI experience (example value: "org.mozilla.firefox").
     *                      Defaults to the Chrome package if not specified.
     */
    protected AWSCognitoAuthSignOutOptions(
            boolean globalSignOut,
            String browserPackage
    ) {
        super(globalSignOut);
        this.browserPackage = browserPackage;
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
                isGlobalSignOut(),
                getBrowserPackage()
        );
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
                    ObjectsCompat.equals(getBrowserPackage(), authSignOutOptions.getBrowserPackage());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignOutOptions{" +
                "isGlobalSignOut=" + isGlobalSignOut() +
                ", browserPackage=" + getBrowserPackage() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private String browserPackage;

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
         * Construct and return the object with the values set in the builder.
         * @return a new instance of AWSCognitoAuthSignOutOptions with the values specified in the builder.
         */
        @NonNull
        public AWSCognitoAuthSignOutOptions build() {
            return new AWSCognitoAuthSignOutOptions(
                    super.isGlobalSignOut(),
                    browserPackage
            );
        }
    }
}
