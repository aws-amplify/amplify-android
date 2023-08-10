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

import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep;

import java.util.Objects;

/**
 * Wraps the result of a reset password operation.
 */
public final class AuthResetPasswordResult {
    private final boolean isPasswordReset;
    private final AuthNextResetPasswordStep nextStep;

    /**
     * Wraps the result of a reset password operation.
     * @param isPasswordReset True if the password value has now been changed. False if it has not yet been changed.
     *                        Check {@link #getNextStep()} for details on additional steps that may be required.
     * @param nextStep Details about the next step in the reset password process (or whether the flow is now done).
     */
    public AuthResetPasswordResult(boolean isPasswordReset, @NonNull AuthNextResetPasswordStep nextStep) {
        this.isPasswordReset = isPasswordReset;
        this.nextStep = Objects.requireNonNull(nextStep);
    }

    /**
     * True if the password value has now been changed. False if it has not yet been changed.
     * Check {@link #getNextStep()} for details on additional steps that may be required.
     * @return True if the password value has now been changed. False if it has not yet been changed.
     */
    public boolean isPasswordReset() {
        return isPasswordReset;
    }

    /**
     * Returns details about the next step in the reset password process (or whether the flow is now done).
     * @return details about the next step in the reset password process (or whether the flow is now done)
     */
    @NonNull
    public AuthNextResetPasswordStep getNextStep() {
        return nextStep;
    }

    /**
     * When overriding, be sure to include isPasswordReset and nextStep in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isPasswordReset(),
                getNextStep()
        );
    }

    /**
     * When overriding, be sure to include isPasswordReset and nextStep in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthResetPasswordResult authSignUpResult = (AuthResetPasswordResult) obj;
            return ObjectsCompat.equals(isPasswordReset(), authSignUpResult.isPasswordReset()) &&
                    ObjectsCompat.equals(getNextStep(), authSignUpResult.getNextStep());
        }
    }

    /**
     * When overriding, be sure to include isPasswordReset and nextStep in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthResetPasswordResult{" +
                "isPasswordReset=" + isPasswordReset() +
                ", nextStep=" + getNextStep() +
                '}';
    }
}