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

import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthSignInState;

public final class AuthSignInResult {
    private final AuthSignInState signInState;
    private final AuthCodeDeliveryDetails codeDeliveryDetails;

    /**
     * Wraps the result of a sign in operation.
     * @param signInState The current state of the sign in process
     * @param codeDeliveryDetails Details about how/whether an MFA code was sent
     */
    public AuthSignInResult(AuthSignInState signInState, AuthCodeDeliveryDetails codeDeliveryDetails) {
        this.signInState = signInState;
        this.codeDeliveryDetails = codeDeliveryDetails;
    }

    /**
     * The current state of the sign in process specifying if it is done or what, if any,
     * additional challenge must be completed in order to finish the sign in operation.
     * @return the current state of the sign in process
     */
    public AuthSignInState getSignInState() {
        return signInState;
    }

    /**
     * Details about how/whether an MFA code was sent.
     * @return Details about how/whether an MFA code was sent
     */
    public AuthCodeDeliveryDetails getCodeDeliveryDetails() {
        return codeDeliveryDetails;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getSignInState(),
                getCodeDeliveryDetails()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthSignInResult authSignInResult = (AuthSignInResult) obj;
            return ObjectsCompat.equals(getSignInState(), authSignInResult.getSignInState()) &&
                    ObjectsCompat.equals(getCodeDeliveryDetails(), authSignInResult.getCodeDeliveryDetails());
        }
    }

    @Override
    public String toString() {
        return "AuthSignInResult{" +
                "signInState=" + signInState +
                ", codeDeliveryDetails=" + codeDeliveryDetails +
                '}';
    }
}
