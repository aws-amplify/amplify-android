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

import com.amplifyframework.auth.AuthState;
import com.amplifyframework.auth.AuthUserState;

import com.amazonaws.auth.AWSCredentials;

import java.util.Objects;

public final class AWSCognitoAuthState extends AuthState {
    private AWSCredentials awsCredentials;
    private String identityId;
    private String accessToken;
    private String idToken;
    private String refreshToken;

    public AWSCognitoAuthState(
            @NonNull AuthUserState state
    ) {
        super(state);
    }

    public AWSCognitoAuthState(
            @NonNull AuthUserState state,
            @Nullable AWSCredentials awsCredentials,
            @Nullable String identityId,
            @Nullable String accessToken,
            @Nullable String idToken,
            @Nullable String refreshToken
    ) {
        super(Objects.requireNonNull(state));

        this.awsCredentials = awsCredentials;
        this.identityId = identityId;
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
    }

    public AWSCredentials getAwsCredentials() {
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

    public void setAwsCredentials(AWSCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
