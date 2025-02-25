/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.RealAWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.helpers.WebAuthnHelper
import com.amplifyframework.auth.cognito.requireIdentityProviderClient

internal class AuthUseCaseFactory(
    private val plugin: RealAWSCognitoAuthPlugin,
    private val authEnvironment: AuthEnvironment,
    private val stateMachine: AuthStateMachine
) {

    fun fetchAuthSession() = FetchAuthSessionUseCase(plugin)

    fun associateWebAuthnCredential() = AssociateWebAuthnCredentialUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine,
        webAuthnHelper = WebAuthnHelper(authEnvironment.context)
    )

    fun listWebAuthnCredentials() = ListWebAuthnCredentialsUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun deleteWebAuthnCredential() = DeleteWebAuthnCredentialUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun rememberDevice() = RememberDeviceUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine,
        environment = authEnvironment
    )

    fun forgetDevice() = ForgetDeviceUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine,
        environment = authEnvironment
    )

    fun fetchDevices() = FetchDevicesUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun fetchUserAttributes() = FetchUserAttributesUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun updateUserAttributes() = UpdateUserAttributesUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun confirmUserAttribute() = ConfirmUserAttributeUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun resendUserAttributeConfirmation() = ResendUserAttributeConfirmationUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun getCurrentUser() = GetCurrentUserUseCase(
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun setupTotp() = SetupTotpUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun verifyTotpSetup() = VerifyTotpSetupUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun updatePassword() = UpdatePasswordUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        fetchAuthSession = fetchAuthSession(),
        stateMachine = stateMachine
    )

    fun resetPassword() = ResetPasswordUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        environment = authEnvironment
    )

    fun confirmResetPassword() = ConfirmResetPasswordUseCase(
        client = authEnvironment.requireIdentityProviderClient(),
        environment = authEnvironment,
        stateMachine = stateMachine
    )
}
