package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.statemachine.codegen.data.HostedUIJsonConfiguration
import com.amplifyframework.statemachine.codegen.data.HostedUISignInOptions

object HostedUISignInOptionsHelper {
    fun createWebSignInOptions(
        options: AuthWebUISignInOptions,
        hostedUIJsonConfig: HostedUIJsonConfiguration
    ) = HostedUISignInOptions.WebSignInOptions(
        scopes = options.scopes.ifEmpty {
            hostedUIJsonConfig.scopes.toList()
        },
        signInQueryParameters = options.signInQueryParameters,
        signOutQueryParameters = options.signOutQueryParameters,
        tokenQueryParameters = options.tokenQueryParameters,
        idpIdentifier = (options as? AWSCognitoAuthWebUISignInOptions)?.idpIdentifier,
        federationProviderName = (options as? AWSCognitoAuthWebUISignInOptions)?.federationProviderName,
        browserPackage = (options as? AWSCognitoAuthWebUISignInOptions)?.browserPackage,
        appClient = hostedUIJsonConfig.appClient,
        appSecret = hostedUIJsonConfig.appSecret,
        domain = hostedUIJsonConfig.domain,
        signInRedirectURI = hostedUIJsonConfig.signInRedirectURI,
        signOutRedirectURI = hostedUIJsonConfig.signOutRedirectURI
    )

    fun createSocialWebSignInOptions(
        options: AuthWebUISignInOptions,
        hostedUIJsonConfig: HostedUIJsonConfiguration,
        authProvider: AuthProvider
    ) = HostedUISignInOptions.SocialWebSignInOptions(
        identityProvider = getIdentityProvider(authProvider),
        scopes = options.scopes,
        signInQueryParameters = options.signInQueryParameters,
        signOutQueryParameters = options.signOutQueryParameters,
        tokenQueryParameters = options.tokenQueryParameters,
        idpIdentifier = (options as? AWSCognitoAuthWebUISignInOptions)?.idpIdentifier,
        federationProviderName = (options as? AWSCognitoAuthWebUISignInOptions)?.federationProviderName,
        browserPackage = (options as? AWSCognitoAuthWebUISignInOptions)?.browserPackage,
        appClient = hostedUIJsonConfig.appClient,
        appSecret = hostedUIJsonConfig.appSecret,
        domain = hostedUIJsonConfig.domain,
        signInRedirectURI = hostedUIJsonConfig.signInRedirectURI,
        signOutRedirectURI = hostedUIJsonConfig.signOutRedirectURI
    )

    private fun getIdentityProvider(authProvider: AuthProvider): String {
        return when (authProvider) {
            AuthProvider.amazon() -> "LoginWithAmazon"
            AuthProvider.facebook() -> "Facebook"
            AuthProvider.google() -> "Google"
            AuthProvider.apple() -> "SignInWithApple"
            else -> authProvider.providerKey
        }
    }
}