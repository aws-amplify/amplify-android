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

// TODO: These values are currently coming from the ChallengeName values in AWS Cognito InitiateAuth call.
//       The team decided to start with these since they are not Cognito specific but consider whether they
//       should be renamed / supplemented. As future plugins need other states, they would be added here as well.
public enum AuthSignInState {
    /**
     * Next challenge is to supply an SMS_MFA_CODE, delivered via SMS.
     */
    SMS_MFA,

    /**
     *  Next challenge is to supply a SOFTWARE_TOKEN_MFA, delivered via software token.
     */
    SOFTWARE_TOKEN_MFA,

    /**
     * If MFA is required, users who do not have at least one of the MFA methods set up are presented with this
     * challenge. The user must set up at least one MFA type to continue to authenticate.
     */
    MFA_SETUP,

    /**
     * Must select an MFA type. Valid MFA options are SMS_MFA for text SMS MFA, and SOFTWARE_TOKEN_MFA for TOTP
     * software token MFA.
     */
    SELECT_MFA_TYPE,

    /**
     * Next challenge is to supply PASSWORD_CLAIM_SIGNATURE, PASSWORD_CLAIM_SECRET_BLOCK, and TIMESTAMP
     * after the client-side SRP calculations.
     */
    PASSWORD_VERIFIER,

    /**
     * This is returned if your custom authentication flow determines that the user should pass another challenge
     * before tokens are issued.
     */
    CUSTOM_CHALLENGE,

    /**
     * If device tracking was enabled on your user pool and the previous challenges were passed, this challenge is
     * returned so that Amazon Cognito can start tracking this device.
     */
    DEVICE_SRP_AUTH,

    /**
     * Similar to PASSWORD_VERIFIER, but for devices only.
     */
    DEVICE_PASSWORD_VERIFIER,

    /**
     * This is returned if you need to authenticate with USERNAME and PASSWORD directly.
     * An app client must be enabled to use this flow.
     */
    ADMIN_NO_SRP_AUTH,

    /**
     * For users which are required to change their passwords after successful first login.
     * This challenge should be passed with NEW_PASSWORD and any other required attributes.
     */
    NEW_PASSWORD_REQUIRED,

    /**
     * The flow is completed and no further steps are needed.
     */
    DONE,

    /**
     * Unknown sign-in state, potentially unsupported state.
     */
    UNKNOWN;

    @NonNull
    public static AuthSignInState fromString(String value) {
        for (AuthSignInState v : values()) {
            if (v.name().equalsIgnoreCase(value)) {
                return v;
            }
        }

        return AuthSignInState.UNKNOWN;
    }
}
