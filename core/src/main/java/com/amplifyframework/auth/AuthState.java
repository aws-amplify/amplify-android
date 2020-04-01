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

public class AuthState {
    private final AuthSignedInStatus signedInStatus;

    /**
     * Stores the current auth state of the category. When cast to a plugin specific object,
     * implementation-specific auth details such as tokens can be retrieved as well.
     * @param signedInStatus What state the user is currently in (e.g. Signed Out, Guest, or Signed In)
     */
    public AuthState(@NonNull AuthSignedInStatus signedInStatus) {
        this.signedInStatus = Objects.requireNonNull(signedInStatus);
    }

    /**
     * Returns the user's current logged in state.
     * @return An enum of the user's current logged in state
     */
    @NonNull
    public AuthSignedInStatus getSignedInStatus() {
        return signedInStatus;
    }

    /**
     * When overriding, be sure to include signedInStatus in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getSignedInStatus()
        );
    }

    /**
     * When overriding, be sure to include signedInStatus in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthState authState = (AuthState) obj;
            return ObjectsCompat.equals(getSignedInStatus(), authState.getSignedInStatus());
        }
    }

    /**
     * When overriding, be sure to include signedInStatus in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append("AuthState { ")
                .append("signedInStatus: ")
                .append(getSignedInStatus())
                .append(" }")
                .toString();
    }
}
