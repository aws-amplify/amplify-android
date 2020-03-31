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
import com.amplifyframework.auth.AuthSignedInStatus;

public final class AuthSignInResult {
    private final AuthSignedInStatus signedInStatus;
    private final AuthCodeDeliveryDetails codeDeliveryDetails;

    /**
     * Wraps the result of a sign in operation.
     * @param signedInStatus What state the user is after the operation finished (e.g. Signed Out, Guest, or Signed In)
     * @param codeDeliveryDetails Details about how/whether an MFA code was sent
     */
    public AuthSignInResult(AuthSignedInStatus signedInStatus, AuthCodeDeliveryDetails codeDeliveryDetails) {
        this.signedInStatus = signedInStatus;
        this.codeDeliveryDetails = codeDeliveryDetails;
    }

    /**
     * The current state of the user after the operation (e.g. Signed Out, Guest, or Signed In).
     * @return the current state of the user after the operation (e.g. Signed Out, Guest, or Signed In)
     */
    public AuthSignedInStatus getSignedInStatus() {
        return signedInStatus;
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
                getSignedInStatus(),
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
            return ObjectsCompat.equals(getSignedInStatus(), authSignInResult.getSignedInStatus()) &&
                    ObjectsCompat.equals(getCodeDeliveryDetails(), authSignInResult.getCodeDeliveryDetails());
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("AuthSignInResult { ")
                .append("signedInStatus: ")
                .append(getSignedInStatus())
                .append(", codeDeliveryDetails: ")
                .append(getCodeDeliveryDetails())
                .append(" }")
                .toString();
    }
}
