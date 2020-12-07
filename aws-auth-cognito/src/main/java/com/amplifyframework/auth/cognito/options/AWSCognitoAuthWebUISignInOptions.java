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

import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Map;

/**
 * Cognito extension of web ui sign in options to add the platform specific fields.
 */
public final class AWSCognitoAuthWebUISignInOptions extends AuthWebUISignInOptions {
    private final String idpIdentifier;
    private final String federationProviderName;
    private final String browserPackage;

    /**
     * Advanced options for signing in via a hosted web ui.
     * @param scopes specify OAUTH scopes
     * @param signInQueryParameters map of custom parameters to send associated with sign in process
     * @param signOutQueryParameters map of custom parameters to send associated with sign out process
     * @param tokenQueryParameters map of custom parameters to send associated with token
     * @param idpIdentifier The IdentityProvider identifier if using multiple instances of same identity provider.
     * @param federationProviderName If federating with Cognito Identity and using a provider lik Auth0 specify the
     *                               provider name, e.g. .auth0.com
     * @param browserPackage Specify which browser package should be used for web sign in (e.g. "org.mozilla.firefox").
     *                       Defaults to the Chrome package if not specified.
     */
    protected AWSCognitoAuthWebUISignInOptions(
            List<String> scopes,
            Map<String, String> signInQueryParameters,
            Map<String, String> signOutQueryParameters,
            Map<String, String> tokenQueryParameters,
            String idpIdentifier,
            String federationProviderName,
            String browserPackage
    ) {
        super(scopes, signInQueryParameters, signOutQueryParameters, tokenQueryParameters);
        this.idpIdentifier = idpIdentifier;
        this.federationProviderName = federationProviderName;
        this.browserPackage = browserPackage;
    }

    /**
     * The IdentityProvider identifier if using multiple instances of same identity provider.
     * @return the IdentityProvider identifier
     */
    @Nullable
    public String getIdpIdentifier() {
        return idpIdentifier;
    }

    /**
     * If federating with Cognito Identity and using a provider lik Auth0 specify the provider name, e.g. .auth0.com
     * @return the provider name
     */
    @Nullable
    public String getFederationProviderName() {
        return federationProviderName;
    }

    /**
     * Optional browser package override to choose a browser app other than Chrome to launch web sign in.
     * @return optional browser package override to choose a browser app other than Chrome to launch web sign in.
     */
    @Nullable
    public String getBrowserPackage() {
        return browserPackage;
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
                getScopes(),
                getSignInQueryParameters(),
                getSignOutQueryParameters(),
                getTokenQueryParameters(),
                getIdpIdentifier(),
                getFederationProviderName(),
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
            AWSCognitoAuthWebUISignInOptions webUISignInOptions = (AWSCognitoAuthWebUISignInOptions) obj;
            return ObjectsCompat.equals(getScopes(), webUISignInOptions.getScopes()) &&
                    ObjectsCompat.equals(getSignInQueryParameters(), webUISignInOptions.getSignInQueryParameters()) &&
                    ObjectsCompat.equals(getSignOutQueryParameters(), webUISignInOptions.getSignOutQueryParameters()) &&
                    ObjectsCompat.equals(getTokenQueryParameters(), webUISignInOptions.getTokenQueryParameters()) &&
                    ObjectsCompat.equals(getIdpIdentifier(), webUISignInOptions.getIdpIdentifier()) &&
                    ObjectsCompat.equals(getFederationProviderName(), webUISignInOptions.getFederationProviderName()) &&
                    ObjectsCompat.equals(getBrowserPackage(), webUISignInOptions.getBrowserPackage());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthWebUISignInOptions{" +
                "scopes=" + getScopes() +
                ", signInQueryParameters=" + getSignInQueryParameters() +
                ", signOutQueryParameters=" + getSignOutQueryParameters() +
                ", tokenQueryParameters=" + getTokenQueryParameters() +
                ", idpIdentifier=" + getIdpIdentifier() +
                ", federationProviderName=" + getFederationProviderName() +
                ", browserPackage=" + getBrowserPackage() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private String idpIdentifier;
        private String federationProviderName;
        private String browserPackage;

        /**
         * Constructs the builder.
         */
        public CognitoBuilder() {
            super();
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
         * The IdentityProvider identifier if using multiple instances of same identity provider.
         * @param idpIdentifier The IdentityProvider identifier if using multiple instances of same identity provider.
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder idpIdentifier(@NonNull String idpIdentifier) {
            this.idpIdentifier = idpIdentifier;
            return getThis();
        }

        /**
         * If federating with Cognito Identity and using a provider lik Auth0 specify the provider name.
         * @param federationProviderName If federating with Cognito Identity and using a provider lik Auth0 specify the
         *                               provider name, e.g. .auth0.com
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder federationProviderName(@NonNull String federationProviderName) {
            this.federationProviderName = federationProviderName;
            return getThis();
        }

        /**
         * This can optionally be set to specify which browser package should perform the sign in action
         * (e.g. "org.mozilla.firefox"). Defaults to the Chrome package if not set.
         *
         * @param browserPackage String specifying the browser package to perform the web sign in action.
         * @return the instance of the builder.
         */
        public CognitoBuilder browserPackage(@NonNull String browserPackage) {
            this.browserPackage = browserPackage;
            return this;
        }

        /**
         * Build the object.
         * @return a new instance of AWSCognitoAuthWebUISignInOptions.
         */
        @NonNull
        public AWSCognitoAuthWebUISignInOptions build() {
            return new AWSCognitoAuthWebUISignInOptions(
                    Immutable.of(super.getScopes()),
                    Immutable.of(super.getSignInQueryParameters()),
                    Immutable.of(super.getSignOutQueryParameters()),
                    Immutable.of(super.getTokenQueryParameters()),
                    idpIdentifier,
                    federationProviderName,
                    browserPackage
            );
        }
    }
}
