/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3;

import androidx.annotation.NonNull;

import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.storage.s3.UserCredentials.IdentityIdSource;
import com.amplifyframework.testutils.sync.SynchronousAuth;

import java.util.Objects;

/**
 * An {@link IdentityIdSource} which looks up an identity ID for a user by asking
 * an instance of {@link SynchronousAuth} for it.
 */
final class AuthIdentityIdSource implements IdentityIdSource {
    private final SynchronousAuth synchronousAuth;

    private AuthIdentityIdSource(SynchronousAuth synchronousAuth) {
        this.synchronousAuth = synchronousAuth;
    }

    static AuthIdentityIdSource create(@NonNull SynchronousAuth synchronousAuth) {
        Objects.requireNonNull(synchronousAuth);
        return new AuthIdentityIdSource(synchronousAuth);
    }

    @NonNull
    @Override
    public String fetchIdentityId(@NonNull String username, @NonNull String password) throws IllegalArgumentException {
        try {
            synchronousAuth.signIn(username, password);
            String identityId = ((AWSCognitoAuthSession) synchronousAuth.fetchAuthSession()).getIdentityId().getValue();
            synchronousAuth.signOut();
            return Objects.requireNonNull(identityId);
        } catch (NullPointerException | AuthException lookupFailure) {
            throw new IllegalArgumentException(lookupFailure);
        }
    }
}
