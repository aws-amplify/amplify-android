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

    @Nullable
    public AWSCredentials getAWSCredentials() {
        return awsCredentials;
    }

    @Nullable
    public String getUserSub() {
        return userSub;
    }

    @Nullable
    public String getIdentityId() {
        return identityId;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getIdToken() {
        return idToken;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

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
        return new StringBuilder()
                .append("AWSCognitoAuthState { ")
                .append("isSignedIn: ")
                .append(isSignedIn())
                .append(", awsCredentials: ")
                .append(getAWSCredentials())
                .append(", userSub: ")
                .append(getUserSub())
                .append(", identityId: ")
                .append(getIdentityId())
                .append(", accessToken: ")
                .append(getAccessToken())
                .append(", idToken: ")
                .append(getIdToken())
                .append(", refreshToken: ")
                .append(getRefreshToken())
                .append(" }")
                .toString();
    }

    @Override
    public AWSCredentials getCredentials() {
        return awsCredentials;
    }

    @Override
    public void refresh() {
        // NO-OP since this is a session snapshot
    }

    public static final class Builder {
        private boolean isSignedIn;
        private AWSCredentials awsCredentials;
        private String userSub;
        private String identityId;
        private String accessToken;
        private String idToken;
        private String refreshToken;

        @NonNull
        public Builder isSignedIn(boolean isSignedIn) {
            this.isSignedIn = isSignedIn;
            return this;
        }

        @NonNull
        public Builder awsCredentials(@Nullable AWSCredentials awsCredentials) {
            this.awsCredentials = awsCredentials;
            return this;
        }

        @NonNull
        public Builder userSub(@Nullable String userSub) {
            this.userSub = userSub;
            return this;
        }

        @NonNull
        public Builder identityId(@Nullable String identityId) {
            this.identityId = identityId;
            return this;
        }

        @NonNull
        public Builder accessToken(@Nullable String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        @NonNull
        public Builder idToken(@Nullable String idToken) {
            this.idToken = idToken;
            return this;
        }

        @NonNull
        public Builder refreshToken(@Nullable String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

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
