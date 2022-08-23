package com.amplifyframework.auth.cognito.helpers

import android.app.Activity
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.statemachine.codegen.data.HostedUIOptions
import com.amplifyframework.statemachine.codegen.data.HostedUIProviderInfo

object HostedUIHelper {

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