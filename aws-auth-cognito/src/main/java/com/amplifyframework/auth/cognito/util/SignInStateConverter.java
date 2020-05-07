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

package com.amplifyframework.auth.cognito.util;

import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.result.step.AuthSignInStep;

import com.amazonaws.mobile.client.results.SignInState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpful util class to convert AWSMobileClient sign in state enum to the Amplify equivalent.
 */
public final class SignInStateConverter {
    private static final Map<SignInState, AuthSignInStep> CONVERT_SIGN_IN_STATE;

    /**
     * Dis-allows instantiation of this class.
     */
    private SignInStateConverter() { }

    static {
        Map<SignInState, AuthSignInStep> convertSignInStateInit = new HashMap<>();
        convertSignInStateInit.put(SignInState.SMS_MFA, AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE);
        convertSignInStateInit.put(SignInState.CUSTOM_CHALLENGE, AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE);
        convertSignInStateInit.put(SignInState.NEW_PASSWORD_REQUIRED, AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD);
        convertSignInStateInit.put(SignInState.DONE, AuthSignInStep.DONE);
        CONVERT_SIGN_IN_STATE = Collections.unmodifiableMap(convertSignInStateInit);
    }

    public static AuthSignInStep getAuthSignInStep(SignInState fromState) throws AuthException {
        AuthSignInStep convertedVal = CONVERT_SIGN_IN_STATE.get(fromState);

        if (convertedVal != null) {
            return convertedVal;
        } else {
            throw new AuthException("Unsupported sign in state",
                "We currently do not support the " + fromState + " state from AWSMobileClient. " +
                    "If this error is reached, please report it as a bug");
        }
    }
}
