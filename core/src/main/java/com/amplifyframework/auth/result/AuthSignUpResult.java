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

import com.amplifyframework.auth.result.step.AuthNextSignUpStep;

public final class AuthSignUpResult {
    private final boolean isSignUpComplete;
    private final AuthNextSignUpStep nextStep;

    /**
     * Wraps the result of a sign up operation.
     * @param isSignUpComplete True if the user is successfully registered, False otherwise. Not that even if the user
     *                         is successfully registered, there still may be additional steps that can be performed
     *                         such as user confirmation. Check {@link #getNextStep()} for this information.
     * @param nextStep Details about the next step in the sign up process (or whether the flow is now done).
     */
    public AuthSignUpResult(boolean isSignUpComplete, AuthNextSignUpStep nextStep) {
        this.isSignUpComplete = isSignUpComplete;
        this.nextStep = nextStep;
    }

    /**
     * True if the user is successfully registered, False otherwise. Note that even if the user
     * is successfully signed up, there still may be additional steps that can be performed
     * such as user confirmation. Check {@link #getNextStep()} for this information.
     * @return True if the user is successfully registered, False otherwise
     */
    public boolean isSignUpComplete() {
        return isSignUpComplete;
    }

    /**
     * Returns details about the next step in the sign up process (or whether the flow is now done).
     * @return details about the next step in the sign up process (or whether the flow is now done)
     */
    public AuthNextSignUpStep getNextStep() {
        return nextStep;
    }

    /**
     * When overriding, be sure to include isSignUpComplete and nextStep in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isSignUpComplete(),
                getNextStep()
        );
    }

    /**
     * When overriding, be sure to include isSignUpComplete and nextStep in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthSignUpResult authSignUpResult = (AuthSignUpResult) obj;
            return ObjectsCompat.equals(isSignUpComplete(), authSignUpResult.isSignUpComplete()) &&
                    ObjectsCompat.equals(getNextStep(), authSignUpResult.getNextStep());
        }
    }

    /**
     * When overriding, be sure to include isSignUpComplete and nextStep in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignUpResult{" +
                "userConfirmed=" + isSignUpComplete() +
                ", nextStep=" + getNextStep() +
                '}';
    }
}
