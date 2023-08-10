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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.result.step.AuthNextSignUpStep;

import java.util.Objects;

/**
 * Wraps the result of a sign up operation.
 */
public final class AuthSignUpResult {
    private final boolean isSignUpComplete;
    private final AuthNextSignUpStep nextStep;
    private final String userId;

    /**
     * Wraps the result of a sign up operation.
     * @param isSignUpComplete True if the user is successfully registered, False otherwise. Not that even if the user
     *                         is successfully registered, there still may be additional steps that can be performed
     *                         such as user confirmation. Check {@link #getNextStep()} for this information.
     * @param nextStep Details about the next step in the sign up process (or whether the flow is now done).
     * @param userId If {@link #isSignUpComplete} is true, this holds the newly registered user's id. Otherwise, null.
     */
    public AuthSignUpResult(boolean isSignUpComplete, @NonNull AuthNextSignUpStep nextStep, @Nullable String userId) {
        this.isSignUpComplete = isSignUpComplete;
        this.nextStep = Objects.requireNonNull(nextStep);
        this.userId = userId;
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
    @NonNull
    public AuthNextSignUpStep getNextStep() {
        return nextStep;
    }

    /**
     * If {@link #isSignUpComplete} is true, this returns the newly registered user's id. Otherwise, null.
     * @return the newly registered user's id if {@link #isSignUpComplete} is true. Otherwise, null.
     */
    @Nullable
    public String getUserId() {
        return userId;
    }

    /**
     * When overriding, be sure to include isSignUpComplete, nextStep, and user in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isSignUpComplete(),
                getNextStep(),
                getUserId()
        );
    }

    /**
     * When overriding, be sure to include isSignUpComplete, nextStep, and user in the comparison.
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
                    ObjectsCompat.equals(getNextStep(), authSignUpResult.getNextStep()) &&
                    ObjectsCompat.equals(getUserId(), authSignUpResult.getUserId());
        }
    }

    /**
     * When overriding, be sure to include isSignUpComplete, nextStep, and user in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignUpResult{" +
                "isSignUpComplete=" + isSignUpComplete() +
                ", nextStep=" + getNextStep() +
                ", userId=" + getUserId() +
                '}';
    }
}