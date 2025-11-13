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

import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;

/**
 * Cognito extension of web ui sign in options to add the platform specific fields.
 */
public final class AWSCognitoAuthWebUISignInOptions extends AuthWebUISignInOptions {
    private final String idpIdentifier;
    private final String browserPackage;
    private final String nonce;
    private final String language;
    private final String loginHint;
    private final List<AuthWebUIPrompt> prompt;
    private final String resource;

    /**
     * Advanced options for signing in via a hosted web ui.
     * @param scopes specify OAUTH scopes
     * @param idpIdentifier The IdentityProvider identifier if using multiple instances of same identity provider.
     * @param browserPackage Specify which browser package should be used for web sign in (e.g. "org.mozilla.firefox").
     *                       Defaults to the Chrome package if not specified.
     * @param preferPrivateSession specifying whether or not to launch web ui in an ephemeral CustomTab.
     * @param nonce random value that can be added to the request, which is included in the ID token
     *              that Amazon Cognito issues.
     * @param language language displayed in user-interactive page
     * @param loginHint username prompt passed to the authorization server
     * @param prompt a list of OIDC parameters that controls authentication behavior for existing sessions.
     * @param resource identifier of a resource that you want to bind to the access token in the `aud` claim.
     */
    @SuppressWarnings("checkstyle:all")
    protected AWSCognitoAuthWebUISignInOptions(
            List<String> scopes,
            String idpIdentifier,
            String browserPackage,
            Boolean preferPrivateSession,
            String nonce,
            String language,
            String loginHint,
            List<AuthWebUIPrompt> prompt,
            String resource
    ) {
        super(scopes, preferPrivateSession);
        this.idpIdentifier = idpIdentifier;
        this.browserPackage = browserPackage;
        this.nonce = nonce;
        this.language = language;
        this.loginHint = loginHint;
        this.prompt = prompt;
        this.resource = resource;
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
     * Optional browser package override to choose a browser app other than Chrome to launch web sign in.
     * @return optional browser package override to choose a browser app other than Chrome to launch web sign in.
     */
    @Nullable
    public String getBrowserPackage() {
        return browserPackage;
    }

    /**
     * Optional A random value that can be added to the request, which is included in the ID token
     * that Amazon Cognito issues. To guard against replay attacks, your app can inspect the nonce claim in the ID
     * token and compare it to the one you generated.
     * @return the nonce value
     */
    @Nullable
    public String getNonce() {
        return nonce;
    }

    /** Optional The language displayed in user-interactive page.
     * For more information, see Managed login localization
     * https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-managed-login.html
     * @return the language value
     */
    @Nullable
    public String getLanguage() {
        return language;
    }

    /** Optional A username prompt passed to the authorization server. You can collect a username, email
     * address or phone number from your user and allow the destination provider to pre-populate the user's
     * sign-in name.
     * @return the login prompt displayed in the username field
     */
    @Nullable
    public String getLoginHint() {
        return loginHint;
    }

    /**
     * Optional An OIDC parameter that controls authentication behavior for existing sessions.
     * @return the prompt value
     */
    @Nullable
    public List<AuthWebUIPrompt> getPrompt() {
        return prompt;
    }

    /**
     * Optional The identifier of a resource that you want to bind to the access token in the `aud`
     * claim. When this parameter is included, Amazon Cognito validates that the value is a URL and
     * sets the audience of the resulting access token to the requested resource. Values for this
     * parameter must begin with "https://", "http://localhost" or a custom URL scheme like "myapp://".
     * @return the resource value
     */
    @Nullable
    public String getResource() {
        return resource;
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
                getIdpIdentifier(),
                getBrowserPackage(),
                getPreferPrivateSession(),
                getNonce(),
                getLanguage(),
                getLoginHint(),
                getPrompt(),
                getResource()
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
                    ObjectsCompat.equals(getIdpIdentifier(), webUISignInOptions.getIdpIdentifier()) &&
                    ObjectsCompat.equals(getBrowserPackage(), webUISignInOptions.getBrowserPackage()) &&
                    ObjectsCompat.equals(getPreferPrivateSession(), webUISignInOptions.getPreferPrivateSession()) &&
                    ObjectsCompat.equals(getNonce(), webUISignInOptions.getNonce()) &&
                    ObjectsCompat.equals(getLanguage(), webUISignInOptions.getLanguage()) &&
                    ObjectsCompat.equals(getLoginHint(), webUISignInOptions.getLoginHint()) &&
                    ObjectsCompat.equals(getPrompt(), webUISignInOptions.getPrompt()) &&
                    ObjectsCompat.equals(getResource(), webUISignInOptions.getResource());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthWebUISignInOptions{" +
                "scopes=" + getScopes() +
                ", idpIdentifier=" + getIdpIdentifier() +
                ", browserPackage=" + getBrowserPackage() +
                ", preferPrivateSession=" + getPreferPrivateSession() +
                ", nonce=" + getNonce() +
                ", language=" + getLanguage() +
                ", loginHint=" + getLoginHint() +
                ", prompt=" + getPrompt() +
                ", resource=" + getResource() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private String idpIdentifier;
        private String browserPackage;
        private String nonce;
        private String language;
        private String loginHint;
        private List<AuthWebUIPrompt> prompt;
        private String resource;

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
         * A random value that can be added to the request, which is included in the ID token
         * that Amazon Cognito issues.
         * @param nonce a random value to be added to the request
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder nonce(@NonNull String nonce) {
            this.nonce = nonce;
            return getThis();
        }

        /** The language displayed in user-interactive page.
         * For more information, see Managed login localization
         * https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-managed-login.html
         * @param language language value
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder language(@NonNull String language) {
            this.language = language;
            return getThis();
        }

        /** A username prompt passed to the authorization server. You can collect a username, email
         * address or phone number from your user and allow the destination provider to pre-populate the user's
         * sign-in name.
         * @param loginHint login prompt to pass to authorization server
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder loginHint(@NonNull String loginHint) {
            this.loginHint = loginHint;
            return getThis();
        }

        /**
         * Optional An OIDC parameter that controls authentication behavior for existing sessions.
         * @param prompt list of AuthWebUIPrompt values
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder prompt(AuthWebUIPrompt... prompt) {
            this.prompt = new ArrayList<>();
            for (AuthWebUIPrompt value : prompt) {
                this.prompt.add(value);
            }
            return getThis();
        }

        /**
         * Optional The identifier of a resource that you want to bind to the access token in the `aud`
         * claim. When this parameter is included, Amazon Cognito validates that the value is a URL and
         * sets the audience of the resulting access token to the requested resource. Values for this
         * parameter must begin with "https://", "http://localhost" or a custom URL scheme like "myapp://".
         * @param resource resource value
         * @return the instance of the builder.
         */
        @NonNull
        public CognitoBuilder resource(@NonNull String resource) {
            this.resource = resource;
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
                    idpIdentifier,
                    browserPackage,
                    super.getPreferPrivateSession(),
                    nonce,
                    language,
                    loginHint,
                    prompt,
                    resource
            );
        }
    }
}
