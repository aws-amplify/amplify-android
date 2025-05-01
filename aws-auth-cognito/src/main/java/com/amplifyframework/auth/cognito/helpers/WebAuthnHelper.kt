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

package com.amplifyframework.auth.cognito.helpers

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.domerrors.DataError
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.exceptions.webauthn.WebAuthnCredentialAlreadyExistsException
import com.amplifyframework.auth.cognito.exceptions.webauthn.WebAuthnFailedException
import com.amplifyframework.auth.cognito.exceptions.webauthn.WebAuthnNotSupportedException
import com.amplifyframework.auth.cognito.exceptions.webauthn.WebAuthnRpMismatchException
import java.lang.ref.WeakReference

internal class WebAuthnHelper(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    private val logger = authLogger()

    suspend fun getCredential(requestJson: String, callingActivity: WeakReference<Activity>): String {
        try {
            // Construct the request for CredentialManager. We're only interested in PublicKey credentials
            val options = GetPublicKeyCredentialOption(requestJson = requestJson)
            val request = GetCredentialRequest(credentialOptions = listOf(options))

            logger.verbose("Prompting user for PassKey authorization")
            val result = credentialManager.getCredential(context = callingActivity.resolveContext(), request = request)

            // Extract the Public Key credential response. This is what we send to Cognito.
            val publicKeyResult = result.credential as? PublicKeyCredential ?: throw WebAuthnFailedException(
                "Android returned wrong credential type",
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            )
            return publicKeyResult.authenticationResponseJson
        } catch (e: GetCredentialException) {
            throw e.toAuthException()
        }
    }

    suspend fun createCredential(requestJson: String, callingActivity: Activity): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Create the request for CredentialManager
                val request = CreatePublicKeyCredentialRequest(requestJson)

                // Create the credential
                logger.verbose("Prompting user to create a PassKey")
                val result: CreateCredentialResponse = credentialManager.createCredential(callingActivity, request)

                // Extract the Public Key registration response. This is what we send to Cognito.
                val publicKeyResult = result as? CreatePublicKeyCredentialResponse ?: throw WebAuthnFailedException(
                    "Android created wrong credential type",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                )
                return publicKeyResult.registrationResponseJson
            } catch (e: CreateCredentialException) {
                throw e.toAuthException()
            }
        } else {
            throw WebAuthnNotSupportedException()
        }
    }

    private fun WeakReference<Activity>.resolveContext(): Context {
        // Use the Activity context if provided. The Activity context will allow the authorization UI to be shown
        // in the same Task instance - if we use the Application context instead it will launch a new Task.
        // Customers should always provide a calling activity when using WebAuthn for the best user experience.
        val activity = get()
        if (activity == null) {
            logger.warn(
                "No Activity context available when accessing device PassKey. This will result in the system " +
                    "UI appearing in a new Task. We recommend setting the callingActivity option when invoking " +
                    "Amplify Auth APIs if you are using WebAuthn."
            )
        }
        return activity ?: context
    }

    private fun CreateCredentialException.toAuthException(): AuthException = when (this) {
        is CreateCredentialCancellationException -> userCancelledException()
        is CreateCredentialProviderConfigurationException -> notSupported()
        is CreateCredentialUnsupportedException -> notSupported()
        is CreatePublicKeyCredentialDomException -> {
            when (this.domError) {
                is NotAllowedError -> userCancelledException()
                is InvalidStateError -> alreadyExists()
                is DataError -> rpMismatch()
                else -> unknownException()
            }
        }
        else -> unknownException()
    }

    private fun GetCredentialException.toAuthException(): AuthException = when (this) {
        is GetCredentialCancellationException -> userCancelledException()
        is GetCredentialProviderConfigurationException -> notSupported()
        is GetCredentialUnsupportedException -> notSupported()
        is GetPublicKeyCredentialDomException -> {
            when (this.domError) {
                is NotAllowedError -> userCancelledException()
                is DataError -> rpMismatch()
                else -> unknownException()
            }
        }
        else -> unknownException()
    }

    // The exception returned when user cancels
    private fun Exception.userCancelledException() = UserCancelledException(
        message = "User cancelled granting access to PassKey",
        recoverySuggestion = "Re-show the previous UI and allow user to try again",
        cause = this
    ).also { logger.verbose("User cancelled the PassKey authorization UI") }

    private fun CreatePublicKeyCredentialException.alreadyExists() = WebAuthnCredentialAlreadyExistsException(this)
    private fun Exception.notSupported() = WebAuthnNotSupportedException(this)
    private fun Exception.rpMismatch() = WebAuthnRpMismatchException(this)

    // The default exception returned when fetching credentials
    private fun CreateCredentialException.unknownException() =
        WebAuthnFailedException("Unable to create the passkey using the Androidx CredentialManager", cause = this)
    private fun GetCredentialException.unknownException() =
        WebAuthnFailedException("Unable to retrieve the passkey from the Androidx CredentialManager", cause = this)
}
