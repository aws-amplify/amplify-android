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

import com.amplifyframework.auth.AuthSignedInStatus;
import com.amplifyframework.auth.AuthState;

import com.amazonaws.auth.AWSCredentials;

import java.util.Objects;

public final class AWSCognitoAuthState extends AuthState {
    private final AWSCredentials awsCredentials;
    private final String identityId;
    private final String accessToken;
    private final String idToken;
    private final String refreshToken;

    private AWSCognitoAuthState(
            AuthSignedInStatus signedInStatus,
            AWSCredentials awsCredentials,
            String identityId,
            String accessToken,
            String idToken,
            String refreshToken
    ) {
        super(signedInStatus);
        this.awsCredentials = awsCredentials;
        this.identityId = identityId;
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
    }

    public AWSCredentials getAWSCredentials() {
        return awsCredentials;
    }

    public String getIdentityId() {
        return identityId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

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
            getAWSCredentials(),
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
            AWSCognitoAuthState cognitoAuthState = (AWSCognitoAuthState) obj;
            return ObjectsCompat.equals(getAWSCredentials(), cognitoAuthState.getAWSCredentials()) &&
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
                .append("awsCredentials: ")
                .append(getAWSCredentials())
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

    public static final class Builder {
        private AuthSignedInStatus signedInStatus;
        private AWSCredentials awsCredentials;
        private String identityId;
        private String accessToken;
        private String idToken;
        private String refreshToken;

        @NonNull
        public Builder signedInStatus(@NonNull AuthSignedInStatus signedInStatus) {
            this.signedInStatus = Objects.requireNonNull(signedInStatus);
            return this;
        }

        @NonNull
        public Builder awsCredentials(@Nullable AWSCredentials awsCredentials) {
            this.awsCredentials = awsCredentials;
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
        public AWSCognitoAuthState build() {
            return new AWSCognitoAuthState(
                    signedInStatus,
                    awsCredentials,
                    identityId,
                    accessToken,
                    idToken,
                    refreshToken
            );
        }
    }
}
