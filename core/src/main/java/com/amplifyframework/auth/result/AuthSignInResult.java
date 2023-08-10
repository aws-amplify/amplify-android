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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.result.step.AuthNextSignInStep;

import java.util.Objects;

/**
 * Wraps the result of a sign up operation.
 */
public final class AuthSignInResult {
    private final boolean isSignedIn;
    private final AuthNextSignInStep nextStep;

    /**
     * Wraps the result of a sign up operation.
     * @param isSignedIn True if the user is successfully authenticated, False otherwise. Check
     *                         {@link #getNextStep()} to see if there are any required or optional steps to still
     *                         be taken in the sign in flow.
     * @param nextStep Details about the next step in the sign in process (or whether the flow is now done).
     */
    public AuthSignInResult(boolean isSignedIn, @NonNull AuthNextSignInStep nextStep) {
        this.isSignedIn = isSignedIn;
        this.nextStep = Objects.requireNonNull(nextStep);
    }

    /**
     * True if the user is successfully authenticated, False otherwise. Check
     * {@link #getNextStep()} to see if there are any required or optional steps to still
     * be taken in the sign in flow.
     * @return True if the user is successfully authenticated, False otherwise
     */
    public boolean isSignedIn() {
        return isSignedIn;
    }

    /**
     * Returns details about the next step in the sign in process (or whether the flow is now done).
     * @return details about the next step in the sign in process (or whether the flow is now done)
     */
    @NonNull
    public AuthNextSignInStep getNextStep() {
        return nextStep;
    }

    /**
     * When overriding, be sure to include isSignedIn and nextStep in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isSignedIn(),
                getNextStep()
        );
    }

    /**
     * When overriding, be sure to include isSignedIn and nextStep in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthSignInResult authSignUpResult = (AuthSignInResult) obj;
            return ObjectsCompat.equals(isSignedIn(), authSignUpResult.isSignedIn()) &&
                    ObjectsCompat.equals(getNextStep(), authSignUpResult.getNextStep());
        }
    }

    /**
     * When overriding, be sure to include isSignedIn and nextStep in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignInResult{" +
                "isSignedIn=" + isSignedIn() +
                ", nextStep=" + getNextStep() +
                '}';
    }
}