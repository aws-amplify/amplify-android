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

package com.amplifyframework.auth.result.step;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.AuthCodeDeliveryDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This object represents all details around the next step in the sign up process. It holds an instance of the
 * {@link AuthSignUpStep} enum to denote the step itself and supplements it with additional details which can
 * optionally accompany it. If there is no next step, {@link #getSignUpStep()} will have a value of DONE.
 */
public final class AuthNextSignUpStep {
    private final AuthSignUpStep signUpStep;
    private final Map<String, String> additionalInfo;
    private final AuthCodeDeliveryDetails codeDeliveryDetails;

    /**
     * Gives details on the next step, if there is one, in the sign up flow.
     * @param signUpStep the next step in the sign up flow (could be optional or required)
     * @param additionalInfo possible extra info to go with the next step (refer to plugin documentation)
     * @param codeDeliveryDetails Details about how a code was sent, if relevant to the current step
     */
    public AuthNextSignUpStep(
            @NonNull AuthSignUpStep signUpStep,
            @NonNull Map<String, String> additionalInfo,
            @Nullable AuthCodeDeliveryDetails codeDeliveryDetails) {
        this.signUpStep = Objects.requireNonNull(signUpStep);
        this.additionalInfo = new HashMap<>();
        this.additionalInfo.putAll(Objects.requireNonNull(additionalInfo));
        this.codeDeliveryDetails = codeDeliveryDetails;
    }

    /**
     * Returns the next step in the sign up flow (could be optional or required).
     * @return the next step in the sign up flow (could be optional or required)
     */
    @NonNull
    public AuthSignUpStep getSignUpStep() {
        return signUpStep;
    }

    /**
     * Returns possible extra info to go with the next step (refer to plugin documentation).
     * @return possible extra info to go with the next step (refer to plugin documentation)
     */
    @Nullable
    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * Details about how a code was sent, if relevant to the current step.
     * @return Details about how a code was sent, if relevant to the current step - null otherwise
     */
    @Nullable
    public AuthCodeDeliveryDetails getCodeDeliveryDetails() {
        return codeDeliveryDetails;
    }

    /**
     * When overriding, be sure to include signUpStep, additionalInfo, and codeDeliveryDetails in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getSignUpStep(),
                getAdditionalInfo(),
                getCodeDeliveryDetails()
        );
    }

    /**
     * When overriding, be sure to include signUpStep, additionalInfo, and codeDeliveryDetails in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthNextSignUpStep authSignUpResult = (AuthNextSignUpStep) obj;
            return ObjectsCompat.equals(getSignUpStep(), authSignUpResult.getSignUpStep()) &&
                    ObjectsCompat.equals(getAdditionalInfo(), authSignUpResult.getAdditionalInfo()) &&
                    ObjectsCompat.equals(getCodeDeliveryDetails(), authSignUpResult.getCodeDeliveryDetails());
        }
    }

    /**
     * When overriding, be sure to include signUpStep, additionalInfo, and codeDeliveryDetails in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthSignUpResult{" +
                "signUpStep=" + getSignUpStep() +
                ", additionalInfo=" + getAdditionalInfo() +
                ", codeDeliveryDetails=" + getCodeDeliveryDetails() +
                '}';
    }
}
