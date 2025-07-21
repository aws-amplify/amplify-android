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
import com.amplifyframework.auth.AuthFactorType;
import com.amplifyframework.auth.MFAType;
import com.amplifyframework.auth.TOTPSetupDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This object represents all details around the next step in the sign in process. It holds an instance of the
 * {@link AuthSignInStep} enum to denote the step itself and supplements it with additional details which can
 * optionally accompany it. If there is no next step, {@link #getSignInStep()} will have a value of DONE.
 */
public final class AuthNextSignInStep {
    private final AuthSignInStep signInStep;
    private final Map<String, String> additionalInfo;
    private final AuthCodeDeliveryDetails codeDeliveryDetails;

    private final TOTPSetupDetails totpSetupDetails;
    private final Set<MFAType> allowedMFATypes;
    private final Set<AuthFactorType> availableFactors;

    /**
     * Gives details on the next step, if there is one, in the sign in flow.
     * @param signInStep the next step in the sign in flow (could be optional or required)
     * @param additionalInfo possible extra info to go with the next step (refer to plugin documentation)
     * @param codeDeliveryDetails Details about how a code was sent, if relevant to the current step
     * @param totpSetupDetails Details to setup TOTP, if relevant to the current step
     * @param allowedMFATypes Set of allowed MFA type, if relevant to the current step
     * @param availableFactors Set of available AuthFactorType to choose from, if relevant to the
     *                         current step
     */
    public AuthNextSignInStep(
            @NonNull AuthSignInStep signInStep,
            @NonNull Map<String, String> additionalInfo,
            @Nullable AuthCodeDeliveryDetails codeDeliveryDetails,
            @Nullable TOTPSetupDetails totpSetupDetails,
            @Nullable Set<MFAType> allowedMFATypes,
            @Nullable Set<AuthFactorType> availableFactors) {
        this.signInStep = Objects.requireNonNull(signInStep);
        this.additionalInfo = new HashMap<>();
        this.additionalInfo.putAll(Objects.requireNonNull(additionalInfo));
        this.codeDeliveryDetails = codeDeliveryDetails;
        this.totpSetupDetails = totpSetupDetails;
        this.allowedMFATypes = allowedMFATypes;
        this.availableFactors = availableFactors;
    }

    /**
     * Returns the next step in the sign in flow (could be optional or required).
     * @return the next step in the sign in flow (could be optional or required)
     */
    @NonNull
    public AuthSignInStep getSignInStep() {
        return signInStep;
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
     * Details about how to setup TOTP.
     * @return Details about how to setup TOTP, if relevant to the current step - null otherwise
     */
    @Nullable
    public TOTPSetupDetails getTotpSetupDetails() {
        return totpSetupDetails;
    }

    /**
     * Set of allowed MFA Types.
     * @return Set of allowed MFA Types, if relevant to the current step - null otherwise
     */
    @Nullable
    public Set<MFAType> getAllowedMFATypes() {
        return allowedMFATypes;
    }

    /**
     * Set of available auth factors.
     * @return Set of available auth factors, if relevant to the current step - null otherwise
     */
    @Nullable
    public Set<AuthFactorType> getAvailableFactors() {
        return availableFactors;
    }

    /**
     * When overriding, be sure to include signInStep, additionalInfo, and codeDeliveryDetails in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getSignInStep(),
                getAdditionalInfo(),
                getCodeDeliveryDetails(),
                getTotpSetupDetails(),
                getAllowedMFATypes(),
                getAvailableFactors()
        );
    }

    /**
     * When overriding, be sure to include signInStep, additionalInfo, and codeDeliveryDetails in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthNextSignInStep authSignUpResult = (AuthNextSignInStep) obj;
            return ObjectsCompat.equals(getSignInStep(), authSignUpResult.getSignInStep()) &&
                    ObjectsCompat.equals(getAdditionalInfo(), authSignUpResult.getAdditionalInfo()) &&
                    ObjectsCompat.equals(getCodeDeliveryDetails(), authSignUpResult.getCodeDeliveryDetails()) &&
                    ObjectsCompat.equals(getTotpSetupDetails(), authSignUpResult.getTotpSetupDetails()) &&
                    ObjectsCompat.equals(getAllowedMFATypes(), authSignUpResult.getAllowedMFATypes()) &&
                    ObjectsCompat.equals(getAvailableFactors(), authSignUpResult.getAvailableFactors());
        }
    }

    /**
     * When overriding, be sure to include signInStep, additionalInfo, and codeDeliveryDetails in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthNextSignInStep{" +
                "signInStep=" + getSignInStep() +
                ", additionalInfo=" + getAdditionalInfo() +
                ", codeDeliveryDetails=" + getCodeDeliveryDetails() +
                ", totpSetupDetails=" + getTotpSetupDetails() +
                ", allowedMFATypes=" + getAllowedMFATypes() +
                ", availableFactors=" + getAvailableFactors() +
                '}';
    }
}
