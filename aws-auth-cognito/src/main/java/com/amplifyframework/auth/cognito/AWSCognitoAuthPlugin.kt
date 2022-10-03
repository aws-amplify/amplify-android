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

package com.amplifyframework.auth.cognito

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.asf.UserContextDataProvider
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import org.json.JSONException
import org.json.JSONObject

/**
 * A Cognito implementation of the Auth Plugin.
 */
class AWSCognitoAuthPlugin : AuthPlugin<AWSCognitoAuthServiceBehavior>() {
    companion object {
        const val AWS_COGNITO_AUTH_LOG_NAMESPACE = "amplify:aws-cognito-auth:%s"
        private const val AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    }

    private val logger =
        Amplify.Logging.forNamespace(AWS_COGNITO_AUTH_LOG_NAMESPACE.format(this::class.java.simpleName))

    @VisibleForTesting
    internal lateinit var realPlugin: RealAWSCognitoAuthPlugin

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            val configuration = AuthConfiguration.fromJson(pluginConfiguration)
            val authEnvironment = AuthEnvironment(
                configuration,
                AWSCognitoAuthServiceBehavior.fromConfiguration(configuration),
                configuration.userPool?.let { UserContextDataProvider(context, it) },
                HostedUIClient.create(context, configuration.oauth, logger),
                logger
            )
            val authStateMachine = AuthStateMachine(authEnvironment)
            val credentialStoreStateMachine = createCredentialStoreStateMachine(configuration, context)
            realPlugin = RealAWSCognitoAuthPlugin(
                configuration,
                authEnvironment,
                authStateMachine,
                credentialStoreStateMachine,
                logger
            )
        } catch (exception: JSONException) {
            throw AuthException(
                "Failed to configure AWSCognitoAuthPlugin.",
                exception,
                "Make sure your amplifyconfiguration.json is valid."
            )
        }
    }

    override fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signUp(username, password, options, onSuccess, onError)
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignUp(username, confirmationCode, onSuccess, onError)
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignUp(username, confirmationCode, options, onSuccess, onError)
    }

    override fun resendSignUpCode(
        username: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendSignUpCode(username, onSuccess, onError)
    }

    override fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendSignUpCode(username, options, onSuccess, onError)
    }

    override fun signIn(
        username: String?,
        password: String?,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signIn(username, password, onSuccess, onError)
    }

    override fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signIn(username, password, options, onSuccess, onError)
    }

    override fun confirmSignIn(
        confirmationCode: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignIn(confirmationCode, onSuccess, onError)
    }

    override fun confirmSignIn(
        confirmationCode: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignIn(confirmationCode, options, onSuccess, onError)
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithSocialWebUI(provider, callingActivity, onSuccess, onError)
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithSocialWebUI(provider, callingActivity, options, onSuccess, onError)
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithWebUI(callingActivity, onSuccess, onError)
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithWebUI(callingActivity, options, onSuccess, onError)
    }

    override fun handleWebUISignInResponse(intent: Intent?) {
        realPlugin.handleWebUISignInResponse(intent)
    }

    override fun fetchAuthSession(
        options: AuthFetchSessionOptions,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.fetchAuthSession(options, onSuccess, onError)
    }

    override fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) {
        realPlugin.fetchAuthSession(onSuccess, onError)
    }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        realPlugin.rememberDevice(onSuccess, onError)
    }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        realPlugin.forgetDevice(onSuccess, onError)
    }

    override fun forgetDevice(
        device: AuthDevice,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.forgetDevice(device, onSuccess, onError)
    }

    override fun fetchDevices(
        onSuccess: Consumer<MutableList<AuthDevice>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.fetchDevices(onSuccess, onError)
    }

    override fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resetPassword(username, options, onSuccess, onError)
    }

    override fun resetPassword(
        username: String,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resetPassword(username, onSuccess, onError)
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmResetPassword(username, newPassword, confirmationCode, options, onSuccess, onError)
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmResetPassword(username, newPassword, confirmationCode, onSuccess, onError)
    }

    override fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updatePassword(oldPassword, newPassword, onSuccess, onError)
    }

    override fun fetchUserAttributes(
        onSuccess: Consumer<MutableList<AuthUserAttribute>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.fetchUserAttributes(onSuccess, onError)
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttribute(attribute, options, onSuccess, onError)
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttribute(attribute, onSuccess, onError)
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttributes(attributes, options, onSuccess, onError)
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttributes(attributes, onSuccess, onError)
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendUserAttributeConfirmationCode(attributeKey, options, onSuccess, onError)
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendUserAttributeConfirmationCode(attributeKey, onSuccess, onError)
    }

    override fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmUserAttribute(attributeKey, confirmationCode, onSuccess, onError)
    }

    override fun getCurrentUser(
        onSuccess: Consumer<AuthUser>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.getCurrentUser(onSuccess, onError)
    }

    override fun signOut(onComplete: Consumer<AuthSignOutResult>) {
        realPlugin.signOut(onComplete)
    }

    override fun signOut(options: AuthSignOutOptions, onComplete: Consumer<AuthSignOutResult>) {
        realPlugin.signOut(options, onComplete)
    }

    override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) {
        realPlugin.deleteUser(onSuccess, onError)
    }

    override fun getEscapeHatch() = realPlugin.escapeHatch()

    override fun getPluginKey() = AWS_COGNITO_AUTH_PLUGIN_KEY

    override fun getVersion() = BuildConfig.VERSION_NAME

    /**
     * Federate to Identity Pool
     * @param authProvider The auth provider you want to federate for (e.g. Facebook, Google, etc.)
     * @param providerToken Provider token to start the federation for
     * @param onSuccess Success callback
     */
    fun federateToIdentityPool(
        authProvider: AuthProvider,
        providerToken: String,
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.federateToIdentityPool(
            authProvider,
            providerToken,
            FederateToIdentityPoolOptions.builder().build(),
            onSuccess,
            onError
        )
    }

    /**
     * Federate to Identity Pool
     * @param authProvider The auth provider you want to federate for (e.g. Facebook, Google, etc.)
     * @param providerToken Provider token to start the federation for
     * @param options Advanced options for federating to identity pool
     * @param onSuccess Success callback
     */
    fun federateToIdentityPool(
        authProvider: AuthProvider,
        providerToken: String,
        options: FederateToIdentityPoolOptions,
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.federateToIdentityPool(authProvider, providerToken, options, onSuccess, onError)
    }

    /**
     * Clear Federation to Identity Pool
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    fun clearFederationToIdentityPool(
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.clearFederationToIdentityPool(onSuccess, onError)
    }

    private fun createCredentialStoreStateMachine(
        configuration: AuthConfiguration,
        context: Context
    ): CredentialStoreStateMachine {
        val awsCognitoAuthCredentialStore = AWSCognitoAuthCredentialStore(context.applicationContext, configuration)
        val legacyCredentialStore = AWSCognitoLegacyCredentialStore(context.applicationContext, configuration)
        val credentialStoreEnvironment =
            CredentialStoreEnvironment(awsCognitoAuthCredentialStore, legacyCredentialStore, logger)
        return CredentialStoreStateMachine(credentialStoreEnvironment)
    }
}
