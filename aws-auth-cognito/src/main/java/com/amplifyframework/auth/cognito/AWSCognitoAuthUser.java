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
import androidx.core.util.Consumer;

import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.core.Action;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;

import java.util.Objects;

public final class AWSCognitoAuthUser extends AuthUser {
    private final AWSMobileClient client;

    /**
     * Object to represent a logged in user with its associated attributes.
     *
     * @param userId   A unique identifier for this user
     * @param username The username used for logging in
     * @param client An instance of AWSMobileClient to implement the various user actions on this object
     */
    public AWSCognitoAuthUser(
            @NonNull String userId,
            @NonNull String username,
            @NonNull AWSMobileClient client
    ) {
        super(userId, username);
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public void changePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword,
            Action onSuccess,
            Consumer<AuthException> onError
    ) {
        client.changePassword(oldPassword, newPassword, new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                onSuccess.call();
            }

            @Override
            public void onError(Exception error) {
                onError.accept(new AuthException(
                        "Failed to change password",
                        error,
                        "See attached exception for more details"
                ));
            }
        });
    }
}
