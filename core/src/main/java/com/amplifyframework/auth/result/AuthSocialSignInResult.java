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

package com.amplifyframework.auth.result;

import androidx.core.util.ObjectsCompat;

public class AuthSocialSignInResult {
    private final boolean isSignedIn;

    /**
     * Wraps the result of a sign in operation.
     * @param isSignedIn true if the user is now authenticated, false otherwise
     */
    public AuthSocialSignInResult(boolean isSignedIn) {
        this.isSignedIn = isSignedIn;
    }

    /**
     * True if the user is now authenticated, false otherwise.
     * @return true if the user is now signed in, false otherwise
     */
    public boolean isSignedIn() {
        return isSignedIn;
    }

    /**
     * When overriding, be sure to include the parent properties in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isSignedIn()
        );
    }

    /**
     * When overriding, be sure to include the parent properties in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthSocialSignInResult authSocialSignInResult = (AuthSocialSignInResult) obj;
            return ObjectsCompat.equals(isSignedIn(), authSocialSignInResult.isSignedIn());
        }
    }

    /**
     * When overriding, be sure to include the parent properties in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignInResult{" +
                "isSignedIn=" + isSignedIn +
                '}';
    }
}
