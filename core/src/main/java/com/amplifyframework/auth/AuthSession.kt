/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth

import androidx.core.util.ObjectsCompat

/**
 * Stores the current auth state of the category. When cast to a plugin specific object,
 * implementation-specific auth details such as tokens can be retrieved as well.
 */
open class AuthSession(
    /**
     * Stores the current auth state of the category. When cast to a plugin specific object,
     * implementation-specific auth details such as tokens can be retrieved as well.
     * @param isSignedIn true if the user has been signed in, false otherwise
     */
    open val isSignedIn: Boolean
) {
    /**
     * When overriding, be sure to include signedInStatus in the hash.
     * @return Hash code of this object
     */
    override fun hashCode() = ObjectsCompat.hash(isSignedIn)

    /**
     * When overriding, be sure to include signedInStatus in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        val authSession = other as AuthSession
        if (isSignedIn != authSession.isSignedIn) return false

        return true
    }

    /**
     * When overriding, be sure to include signedInStatus in the output string.
     * @return A string representation of the object
     */
    override fun toString() = "AuthSession{isSignedIn=$isSignedIn}"
}
