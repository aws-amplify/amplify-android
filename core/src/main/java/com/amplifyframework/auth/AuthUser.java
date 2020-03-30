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

public final class AuthUser {
    private String userId;
    private String username;

    /**
     * Object to represent a logged in user with its associated attributes
     * @param userId A unique identifier for this user
     * @param username The username used for logging in
     */
    public AuthUser(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    /**
     * A unique identifier for this user.
     * @return a unique identifier for this user
     */
    public String getUserId() {
        return userId;
    }

    /**
     * The username used for logging in.
     * @return the username used for logging in
     */
    public String getUsername() {
        return username;
    }
}
