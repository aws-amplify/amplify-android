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
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

public final class AuthUser {
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

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getUserId(),
                getUsername()
        );
    }

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

    @Override
    public String toString() {
        return "AuthUser{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
