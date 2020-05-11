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

package com.amplifyframework.auth.cognito;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.AuthSession;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

/**
 * Cognito extension of AuthSession containing AWS Cognito specific tokens.
 */
public final class AWSCognitoAuthSession extends AuthSession implements AWSCredentialsProvider {
    private final AWSCredentials awsCredentials;
    private final String userSub;
    private final String identityId;
    private final String accessToken;
    private final String idToken;
    private final String refreshToken;

    private AWSCognitoAuthSession(
            boolean isSignedIn,
            AWSCredentials awsCredentials,
            String userSub,
            String identityId,
            String accessToken,
            String idToken,
            String refreshToken
    ) {
        super(isSignedIn);
        this.awsCredentials = awsCredentials;
        this.userSub = userSub;
        this.identityId = identityId;
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
    }

    /**
     * AWS Credentials from Identity Pools.
     * @return AWS Credentials from Identity Pools.
     */
    @Nullable
    public AWSCredentials getAWSCredentials() {
        return awsCredentials;
    }

    /**
     * User sub.
     * @return user sub
     */
    @Nullable
    public String getUserSub() {
        return userSub;
    }

    /**
     * Identity id.
     * @return identity id.
     */
    @Nullable
    public String getIdentityId() {
        return identityId;
    }

    /**
     * Access token.
     * @return Access token.
     */
    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Id token.
     * @return Id token.
     */
    @Nullable
    public String getIdToken() {
        return idToken;
    }

    /**
     * Refresh token.
     * @return refresh token.
     */
    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Builder.
     * @return builder.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
            isSignedIn(),
            getAWSCredentials(),
            getUserSub(),
            getIdentityId(),
            getAccessToken(),
            getIdToken(),
            getRefreshToken()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthSession cognitoAuthState = (AWSCognitoAuthSession) obj;
            return ObjectsCompat.equals(isSignedIn(), cognitoAuthState.isSignedIn()) &&
                    ObjectsCompat.equals(getAWSCredentials(), cognitoAuthState.getAWSCredentials()) &&
                    ObjectsCompat.equals(getUserSub(), cognitoAuthState.getUserSub()) &&
                    ObjectsCompat.equals(getIdentityId(), cognitoAuthState.getIdentityId()) &&
                    ObjectsCompat.equals(getAccessToken(), cognitoAuthState.getAccessToken()) &&
                    ObjectsCompat.equals(getIdToken(), cognitoAuthState.getIdToken()) &&
                    ObjectsCompat.equals(getRefreshToken(), cognitoAuthState.getRefreshToken());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSession{" +
                "isSignedIn=" + isSignedIn() +
                ", awsCredentials=" + awsCredentials +
                ", userSub='" + userSub + '\'' +
                ", identityId='" + identityId + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", idToken='" + idToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }

    @Override
    public AWSCredentials getCredentials() {
        return awsCredentials;
    }

    @Override
    public void refresh() {
        // NO-OP since this is a session snapshot
    }

    /**
     * The builder for this class.
     */
    public static final class Builder {
        private boolean isSignedIn;
        private AWSCredentials awsCredentials;
        private String userSub;
        private String identityId;
        private String accessToken;
        private String idToken;
        private String refreshToken;

        /**
         * Will be replaced in next PR.
         * @param isSignedIn Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder isSignedIn(boolean isSignedIn) {
            this.isSignedIn = isSignedIn;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @param awsCredentials Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder awsCredentials(@Nullable AWSCredentials awsCredentials) {
            this.awsCredentials = awsCredentials;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @param userSub Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder userSub(@Nullable String userSub) {
            this.userSub = userSub;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @param identityId Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder identityId(@Nullable String identityId) {
            this.identityId = identityId;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @param accessToken Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder accessToken(@Nullable String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @param idToken Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder idToken(@Nullable String idToken) {
            this.idToken = idToken;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @param refreshToken Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public Builder refreshToken(@Nullable String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        /**
         * Will be replaced in next PR.
         * @return Will be replaced in next PR.
         */
        @NonNull
        public AWSCognitoAuthSession build() {
            return new AWSCognitoAuthSession(
                    isSignedIn,
                    awsCredentials,
                    userSub,
                    identityId,
                    accessToken,
                    idToken,
                    refreshToken
            );
        }
    }
}
