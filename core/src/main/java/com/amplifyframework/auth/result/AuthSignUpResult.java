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

    public AuthSignUpResult(boolean userConfirmed, AuthCodeDeliveryDetails codeDeliveryDetails) {
        this.userConfirmed = userConfirmed;
        this.codeDeliveryDetails = codeDeliveryDetails;
    }

    public boolean isUserConfirmed() {
        return userConfirmed;
    }

    public AuthCodeDeliveryDetails getCodeDeliveryDetails() {
        return codeDeliveryDetails;
    }
}
