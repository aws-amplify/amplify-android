/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.statemachine.codegen.data.HostedUIOptions
import com.amplifyframework.statemachine.codegen.data.HostedUIProviderInfo

internal object HostedUIHelper {

    fun createHostedUIOptions(
        callingActivity: Activity,
        authProvider: AuthProvider?,
        options: AuthWebUISignInOptions
    ): HostedUIOptions = HostedUIOptions(
        callingActivity = callingActivity,
        scopes = options.scopes,
        providerInfo = HostedUIProviderInfo(
            authProvider = authProvider,
            idpIdentifier = (options as? AWSCognitoAuthWebUISignInOptions)?.idpIdentifier
        ),
        browserPackage = (options as? AWSCognitoAuthWebUISignInOptions)?.browserPackage
    )

    /**
     * Selects a redirect URI from the list, preferring a non-HTTP/HTTPS URI if available.
     * If no suitable URI is found, falls back to the first URI in the list.
     *
     * @param redirectUris List of redirect URIs
     * @return The selected redirect URI, or first if not empty
     */
    fun selectRedirectUri(redirectUris: List<String>): String? {
        if (redirectUris.isEmpty()) return null

        // First try to find a non-HTTP/HTTPS URI (app scheme URI)
        val nonWebUri = redirectUris.find { uri ->
            val scheme = uri.substringBefore("://", "").lowercase()
            scheme != "http" && scheme != "https" && scheme.isNotEmpty()
        }

        // Return the non-web URI if found, otherwise use the first URI
        return nonWebUri ?: redirectUris.firstOrNull()
    }
}
