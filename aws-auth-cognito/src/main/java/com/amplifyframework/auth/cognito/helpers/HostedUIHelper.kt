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
    ): HostedUIOptions {
        return HostedUIOptions(
            callingActivity = callingActivity,
            scopes = options.scopes,
            providerInfo = HostedUIProviderInfo(
                authProvider = authProvider,
                idpIdentifier = (options as? AWSCognitoAuthWebUISignInOptions)?.idpIdentifier,
                federationProviderName = (options as? AWSCognitoAuthWebUISignInOptions)?.federationProviderName
            ),
            browserPackage = (options as? AWSCognitoAuthWebUISignInOptions)?.browserPackage
        )
    }
}
