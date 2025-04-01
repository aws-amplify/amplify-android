/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.EmailMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SmsMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.setUserMfaPreference
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.MFAPreference
import com.amplifyframework.auth.cognito.UserMFAPreference
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState

internal class UpdateMfaPreferenceUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val fetchMfaPreference: FetchMfaPreferenceUseCase,
    private val stateMachine: AuthStateMachine
) {
    suspend fun execute(sms: MFAPreference?, totp: MFAPreference?, email: MFAPreference?) {
        if (sms == null && totp == null && email == null) {
            throw InvalidParameterException("No mfa settings given")
        }

        stateMachine.requireSignedInState()
        val token = fetchAuthSession.execute().requireAccessToken()

        // If none of the params are marked as preferred then keep the existing preferred property
        val keepExistingPreference = !(
            sms?.mfaPreferred == true ||
                totp?.mfaPreferred == true ||
                email?.mfaPreferred == true
            )
        val existingPreference = fetchMfaPreference.execute()

        client.setUserMfaPreference {
            accessToken = token
            smsMfaSettings = sms?.let {
                val preferred = isPreferred(MFAType.SMS, sms, keepExistingPreference, existingPreference)
                SmsMfaSettingsType {
                    enabled = it.mfaEnabled
                    preferredMfa = preferred
                }
            }
            softwareTokenMfaSettings = totp?.let {
                val preferred = isPreferred(MFAType.TOTP, totp, keepExistingPreference, existingPreference)
                SoftwareTokenMfaSettingsType {
                    enabled = it.mfaEnabled
                    preferredMfa = preferred
                }
            }
            emailMfaSettings = email?.let {
                val preferred = isPreferred(MFAType.EMAIL, email, keepExistingPreference, existingPreference)
                EmailMfaSettingsType {
                    enabled = it.mfaEnabled
                    preferredMfa = preferred
                }
            }
        }
    }

    // If preference.mfaPreferred is set then we use that. Otherwise, we will keep the existing preference if
    // keepExistingPreference is true and the preference is enabled
    private fun isPreferred(
        type: MFAType,
        preference: MFAPreference,
        keepExistingPreference: Boolean,
        existingPreference: UserMFAPreference
    ): Boolean = preference.mfaPreferred ?: (
        keepExistingPreference &&
            existingPreference.preferred == type &&
            preference.mfaEnabled
        )
}
