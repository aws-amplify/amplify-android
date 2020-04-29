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

package com.amplifyframework.auth;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Action;

import java.util.Objects;

public abstract class AuthUser {
    private String userId;
    private String username;

    /**
     * Object to represent a logged in user with its associated attributes.
     * @param userId A unique identifier for this user
     * @param username The username used for logging in
     */
    public AuthUser(@NonNull String userId, @NonNull String username) {
        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
    }

    /**
     * A unique identifier for this user.
     * @return a unique identifier for this user
     */
    @NonNull
    public String getUserId() {
        return userId;
    }

    /**
     * The username used for logging in.
     * @return the username used for logging in
     */
    @NonNull
    public String getUsername() {
        return username;
    }

    /**
     * Interface for changing the password of an existing user.
     * @param oldPassword The user's existing password
     * @param newPassword The new password desired on the user account
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    public abstract void changePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword,
            Action onSuccess,
            Consumer<AuthException> onError
    );

    /**
     * When overriding, be sure to include userId and username in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getUserId(),
                getUsername()
        );
    }

    /**
     * When overriding, be sure to include userId and username in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthUser authUser = (AuthUser) obj;
            return ObjectsCompat.equals(getUserId(), authUser.getUserId()) &&
                    ObjectsCompat.equals(getUsername(), authUser.getUsername());
        }
    }

    /**
     * When overriding, be sure to include userId and username in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthUser{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
