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

public class AuthSession {
    private final boolean isSignedIn;

    /**
     * Stores the current auth state of the category. When cast to a plugin specific object,
     * implementation-specific auth details such as tokens can be retrieved as well.
     * @param isSignedIn true if the user has been signed in, false otherwise
     */
    public AuthSession(boolean isSignedIn) {
        this.isSignedIn = isSignedIn;
    }

    /**
     * Returns true if the user has been signed in, false otherwise.
     * @return true if the user has been signed in, false otherwise
     */
    @NonNull
    public boolean isSignedIn() {
        return isSignedIn;
    }

    /**
     * When overriding, be sure to include signedInStatus in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isSignedIn()
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
            AuthSession authSession = (AuthSession) obj;
            return ObjectsCompat.equals(isSignedIn(), authSession.isSignedIn());
        }
    }

    /**
     * When overriding, be sure to include signedInStatus in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSession{" +
                "isSignedIn=" + isSignedIn +
                '}';
    }
}
