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

import com.amplifyframework.auth.AuthCodeDeliveryDetails;

public final class AuthSignUpResult {
    private final boolean userConfirmed;
    private final AuthCodeDeliveryDetails codeDeliveryDetails;

    /**
     * Wraps the result of a sign up operation.
     * @param userConfirmed True if the user has been confirmed, False otherwise
     * @param codeDeliveryDetails Details about how/whether a confirmation code was sent
     */
    public AuthSignUpResult(boolean userConfirmed, AuthCodeDeliveryDetails codeDeliveryDetails) {
        this.userConfirmed = userConfirmed;
        this.codeDeliveryDetails = codeDeliveryDetails;
    }

    /**
     * True if the user has been confirmed, False otherwise.
     * @return True if the user has been confirmed, False otherwise
     */
    public boolean isUserConfirmed() {
        return userConfirmed;
    }

    /**
     * Details about how/whether a confirmation code was sent.
     * @return Details about how/whether a confirmation code was sent
     */
    public AuthCodeDeliveryDetails getCodeDeliveryDetails() {
        return codeDeliveryDetails;
    }
}
