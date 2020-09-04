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

import com.amplifyframework.auth.result.step.AuthNextUpdateAttributeStep;

import java.util.Objects;

/**
 * Wraps the result of a user attribute operation.
 */
public class AuthUpdateAttributeResult {
    private final boolean isUpdated;
    private final AuthNextUpdateAttributeStep nextStep;

    /**
     * Wraps the result of a user attribute operation.
     * @param isUpdated True if the user attribute has now been updated, False otherwise.
     *                  Check {@link #getNextStep()} for details on additional steps that may be required.
     * @param nextStep Details about the next step in the sign up process (or whether the flow is now done).
     */
    public AuthUpdateAttributeResult(boolean isUpdated, @NonNull AuthNextUpdateAttributeStep nextStep) {
        this.isUpdated = isUpdated;
        this.nextStep = Objects.requireNonNull(nextStep);
    }

    /**
     * True if the user attribute has now been updated. False if it has not yet been updated.
     * Check {@link #getNextStep()} for details on additional steps that may be required.
     * @return True if the user attribute value has now been updated. False if it has not yet been updated.
     */
    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * Returns details about the next step in the user attribute update process (or whether the flow is now done).
     * @return details about the next step in the user attribute update process (or whether the flow is now done)
     */
    @NonNull
    public AuthNextUpdateAttributeStep getNextStep() {
        return nextStep;
    }

    /**
     * When overriding, be sure to include isUpdated and nextStep in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                isUpdated(),
                getNextStep()
        );
    }

    /**
     * When overriding, be sure to include isUpdated and nextStep in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthUpdateAttributeResult authUpdateAttributeResult = (AuthUpdateAttributeResult) obj;
            return ObjectsCompat.equals(isUpdated(), authUpdateAttributeResult.isUpdated()) &&
                    ObjectsCompat.equals(getNextStep(), authUpdateAttributeResult.getNextStep());
        }
    }

    /**
     * When overriding, be sure to include isUpdated and nextStep in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthUpdateAttributeResult{" +
                "isUpdated=" + isUpdated() +
                ", nextStep=" + getNextStep() +
                '}';
    }
}
