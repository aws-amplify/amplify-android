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
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * Wraps the various Cognito User Pool tokens.
 */
public final class AWSCognitoUserPoolTokens {
    private final String accessToken;
    private final String idToken;
    private final String refreshToken;

    /**
     * Wraps the various Cognito User Pool tokens.
     * @param accessToken the access JWT token in its encoded string form.
     * @param idToken the ID JWT token in its encoded string form.
     * @param refreshToken the refresh JWT token in its encoded string form.
     */
    public AWSCognitoUserPoolTokens(
            @NonNull String accessToken,
            @NonNull String idToken,
            @NonNull String refreshToken) {
        this.accessToken = Objects.requireNonNull(accessToken);
        this.idToken = Objects.requireNonNull(idToken);
        this.refreshToken = Objects.requireNonNull(refreshToken);
    }

    /**
     * Returns the access JWT token in its encoded string form.
     * @return the access JWT token in its encoded string form.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Returns the ID JWT token in its encoded string form.
     * @return the ID JWT token in its encoded string form.
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Returns the refresh JWT token in its encoded string form.
     * @return the refresh JWT token in its encoded string form.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
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
            AWSCognitoUserPoolTokens userPoolTokens = (AWSCognitoUserPoolTokens) obj;
            return ObjectsCompat.equals(getAccessToken(), userPoolTokens.getAccessToken()) &&
                    ObjectsCompat.equals(getIdToken(), userPoolTokens.getIdToken()) &&
                    ObjectsCompat.equals(getRefreshToken(), userPoolTokens.getRefreshToken());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoUserPoolTokens{" +
                "accessToken=" + getAccessToken() +
                ", idToken=" + getIdToken() +
                ", refreshToken=" + getRefreshToken() +
                '}';
    }
}
