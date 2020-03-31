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

public class AuthState {
    private final AuthSignedInStatus signedInStatus;

    /**
     * Stores the current auth state of the category. When cast to a plugin specific object,
     * implementation-specific auth details such as tokens can be retrieved as well.
     * @param signedInStatus What state the user is currently in (e.g. Signed Out, Guest, or Signed In)
     */
    public AuthState(AuthSignedInStatus signedInStatus) {
        this.signedInStatus = signedInStatus;
    }

    /**
     * Returns the user's current logged in state.
     * @return An enum of the user's current logged in state
     */
    public AuthSignedInStatus getSignedInStatus() {
        return signedInStatus;
    }
}
