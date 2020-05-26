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

import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.result.AuthSessionResult;

import com.amazonaws.auth.AWSCredentials;

import java.util.Objects;

/**
 * Cognito extension of AuthSession containing AWS Cognito specific tokens.
 */
public final class AWSCognitoAuthSession extends AuthSession {
    private final AuthSessionResult<String> identityId;
    private final AuthSessionResult<AWSCredentials> awsCredentials;
    private final AuthSessionResult<String> userSub;
    private final AuthSessionResult<AWSCognitoUserPoolTokens> userPoolTokens;

    /**
     * Cognito extension of AuthSession containing AWS Cognito specific tokens.
     * @param isSignedIn Are you currently in a signed in state (an AuthN indicator to be technical)
     * @param awsCredentials The credentials which come from Identity Pool
     * @param userSub The id which comes from User Pools
     * @param identityId The id which comes from Identity Pools
     * @param userPoolTokens The tokens which come from User Pools (access, id, refresh tokens)
     */
    public AWSCognitoAuthSession(
            boolean isSignedIn,
            @NonNull AuthSessionResult<String> identityId,
            @NonNull AuthSessionResult<AWSCredentials> awsCredentials,
            @NonNull AuthSessionResult<String> userSub,
            @NonNull AuthSessionResult<AWSCognitoUserPoolTokens> userPoolTokens
    ) {
        super(isSignedIn);
        this.identityId = Objects.requireNonNull(identityId);
        this.awsCredentials = Objects.requireNonNull(awsCredentials);
        this.userSub = Objects.requireNonNull(userSub);
        this.userPoolTokens = Objects.requireNonNull(userPoolTokens);
    }

    /**
     * The credentials which come from Identity Pool.
     * @return the credentials which come from Identity Pool
     */
    @NonNull
    public AuthSessionResult<AWSCredentials> getAWSCredentials() {
        return awsCredentials;
    }

    /**
     * The id which comes from User Pools.
     * @return the id which comes from User Pools
     */
    @NonNull
    public AuthSessionResult<String> getUserSub() {
        return userSub;
    }

    /**
     * The id which comes from Identity Pools.
     * @return the id which comes from Identity Pools
     */
    @NonNull
    public AuthSessionResult<String> getIdentityId() {
        return identityId;
    }

    /**
     * The tokens which come from User Pools (access, id, refresh tokens).
     * @return the tokens which come from User Pools (access, id, refresh tokens)
     */
    @NonNull
    public AuthSessionResult<AWSCognitoUserPoolTokens> getUserPoolTokens() {
        return userPoolTokens;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
            isSignedIn(),
            getAWSCredentials(),
            getUserSub(),
            getIdentityId(),
            getUserPoolTokens()
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
                    ObjectsCompat.equals(getUserPoolTokens(), cognitoAuthState.getUserPoolTokens());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSession{" +
                "isSignedIn=" + isSignedIn() +
                ", awsCredentials=" + getAWSCredentials() +
                ", userSub='" + getUserSub() + '\'' +
                ", identityId='" + getIdentityId() + '\'' +
                ", userPoolTokens='" + getUserPoolTokens() + '\'' +
                '}';
    }
}
