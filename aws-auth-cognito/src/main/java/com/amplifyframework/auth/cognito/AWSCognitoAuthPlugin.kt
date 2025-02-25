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
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AWSCognitoAuthMetadataType
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.cognito.asf.UserContextDataProvider
import com.amplifyframework.auth.cognito.helpers.authLogger
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthVerifyTOTPSetupOptions
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.cognito.usecases.AuthUseCaseFactory
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.options.AuthAssociateWebAuthnCredentialsOptions
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthDeleteWebAuthnCredentialOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthListWebAuthnCredentialsOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthListWebAuthnCredentialsResult
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.configuration.AmplifyOutputsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * A Cognito implementation of the Auth Plugin.
 */
class AWSCognitoAuthPlugin : AuthPlugin<AWSCognitoAuthService>() {
    companion object {
        const val AWS_COGNITO_AUTH_LOG_NAMESPACE = "amplify:aws-cognito-auth:%s"
        private const val AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    }

    private val logger = authLogger()

    @VisibleForTesting
    internal lateinit var realPlugin: RealAWSCognitoAuthPlugin

    @VisibleForTesting
    internal lateinit var useCaseFactory: AuthUseCaseFactory

    private val pluginScope = CoroutineScope(Job() + Dispatchers.Default)
    private val queueFacade: KotlinAuthFacadeInternal by lazy {
        KotlinAuthFacadeInternal(realPlugin)
    }

    private val queueChannel = Channel<Job>(capacity = Channel.UNLIMITED).apply {
        pluginScope.launch {
            consumeEach { it.join() }
        }
    }

    // This function is used for versions of the Authenticator component <= 1.1.0 to get the configuration values needed
    // to configure the Authenticator UI. Starting in 1.2.0 it uses getAuthConfiguration() instead. In order to support
    // older Authenticator versions we translate the config - whether it comes from Gen1 or Gen2 - back into Gen1 JSON
    @InternalAmplifyApi
    @Deprecated("Use getAuthConfiguration instead", replaceWith = ReplaceWith("getAuthConfiguration()"))
    fun getPluginConfiguration(): JSONObject = getAuthConfiguration().toGen1Json()

    @InternalAmplifyApi
    fun getAuthConfiguration() = realPlugin.configuration

    @InternalAmplifyApi
    fun addToUserAgent(type: AWSCognitoAuthMetadataType, value: String) {
        realPlugin.addToUserAgent(type, value)
    }

    private fun Exception.toAuthException(): AuthException = if (this is AuthException) {
        this
    } else {
        CognitoAuthExceptionConverter.lookup(
            error = this,
            fallbackMessage = "An unclassified error prevented this operation."
        )
    }

    override fun initialize(context: Context) {
        realPlugin.initialize()
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            configure(AuthConfiguration.fromJson(pluginConfiguration), context)
        } catch (exception: Exception) {
            throw ConfigurationException(
                "Failed to configure AWSCognitoAuthPlugin.",
                "Make sure your amplifyconfiguration.json is valid.",
                exception
            )
        }
    }

    @InternalAmplifyApi
    override fun configure(amplifyOutputs: AmplifyOutputsData, context: Context) {
        try {
            configure(AuthConfiguration.from(amplifyOutputs), context)
        } catch (exception: Exception) {
            throw ConfigurationException(
                "Failed to configure AWSCognitoAuthPlugin.",
                "Make sure your amplify_outputs.json is valid.",
                exception
            )
        }
    }

    private fun configure(configuration: AuthConfiguration, context: Context) {
        val credentialStoreClient = CredentialStoreClient(configuration, context, logger)
        val authEnvironment = AuthEnvironment(
            context,
            configuration,
            AWSCognitoAuthService.fromConfiguration(configuration),
            credentialStoreClient,
            configuration.userPool?.let { UserContextDataProvider(context, it.poolId!!, it.appClient!!) },
            HostedUIClient.create(context, configuration.oauth, logger),
            logger
        )

        val authStateMachine = AuthStateMachine(authEnvironment)
        realPlugin = RealAWSCognitoAuthPlugin(
            configuration,
            authEnvironment,
            authStateMachine,
            logger
        )

        useCaseFactory = AuthUseCaseFactory(realPlugin, authEnvironment, authStateMachine)

        blockQueueChannelWhileConfiguring()
    }

    // Auth configuration is an async process. Wait until the state machine is in a settled state before attempting
    // to process any customer calls
    private fun blockQueueChannelWhileConfiguring() {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                realPlugin.suspendWhileConfiguring()
            }
        )
    }

    override fun signUp(
        username: String,
        password: String?,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signUp(username, password, options) }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.confirmSignUp(username, confirmationCode) }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.confirmSignUp(username, confirmationCode, options) }

    override fun resendSignUpCode(
        username: String,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.resendSignUpCode(username) }

    override fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.resendSignUpCode(username, options) }

    override fun signIn(
        username: String?,
        password: String?,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signIn(username, password) }

    override fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signIn(username, password, options) }

    override fun confirmSignIn(
        challengeResponse: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.confirmSignIn(challengeResponse) }

    override fun confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.confirmSignIn(challengeResponse, options) }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signInWithSocialWebUI(provider, callingActivity) }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signInWithSocialWebUI(provider, callingActivity, options) }

    override fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signInWithWebUI(callingActivity) }

    override fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.signInWithWebUI(callingActivity, options) }

    override fun handleWebUISignInResponse(intent: Intent?) {
        queueFacade.handleWebUISignInResponse(intent)
    }

    override fun fetchAuthSession(
        options: AuthFetchSessionOptions,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.fetchAuthSession(options) }

    override fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { queueFacade.fetchAuthSession() }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.rememberDevice().execute() }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.forgetDevice().execute() }

    override fun forgetDevice(device: AuthDevice, onSuccess: Action, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.forgetDevice().execute(device) }

    override fun fetchDevices(onSuccess: Consumer<List<AuthDevice>>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.fetchDevices().execute() }

    override fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.resetPassword(username, options) }

    override fun resetPassword(
        username: String,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.resetPassword(username) }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) {
        useCaseFactory.confirmResetPassword().execute(username, newPassword, confirmationCode, options)
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) {
        useCaseFactory.confirmResetPassword().execute(username, newPassword, confirmationCode)
    }

    override fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.updatePassword().execute(oldPassword, newPassword) }

    override fun fetchUserAttributes(onSuccess: Consumer<List<AuthUserAttribute>>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.fetchUserAttributes().execute() }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.updateUserAttributes().execute(attribute, options) }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.updateUserAttributes().execute(attribute) }

    override fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions,
        onSuccess: Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.updateUserAttributes().execute(attributes, options) }

    override fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        onSuccess: Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.updateUserAttributes().execute(attributes) }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.resendUserAttributeConfirmation().execute(attributeKey, options) }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.resendUserAttributeConfirmation().execute(attributeKey) }

    override fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.confirmUserAttribute().execute(attributeKey, confirmationCode) }

    override fun getCurrentUser(onSuccess: Consumer<AuthUser>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.getCurrentUser().execute() }

    override fun signOut(onComplete: Consumer<AuthSignOutResult>) = enqueue(
        onComplete,
        onError = ::throwIt
    ) { queueFacade.signOut() }

    override fun signOut(options: AuthSignOutOptions, onComplete: Consumer<AuthSignOutResult>) = enqueue(
        onComplete,
        onError = ::throwIt
    ) { queueFacade.signOut(options) }

    override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) = enqueue(onSuccess, onError) {
        queueFacade.deleteUser()
    }

    override fun setUpTOTP(onSuccess: Consumer<TOTPSetupDetails>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { useCaseFactory.setupTotp().execute() }

    override fun verifyTOTPSetup(code: String, onSuccess: Action, onError: Consumer<AuthException>) {
        verifyTOTPSetup(code, AWSCognitoAuthVerifyTOTPSetupOptions.CognitoBuilder().build(), onSuccess, onError)
    }

    override fun verifyTOTPSetup(
        code: String,
        options: AuthVerifyTOTPSetupOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.verifyTotpSetup().execute(code, options) }

    override fun associateWebAuthnCredential(
        callingActivity: Activity,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = associateWebAuthnCredential(
        callingActivity,
        AuthAssociateWebAuthnCredentialsOptions.defaults(),
        onSuccess,
        onError
    )

    override fun associateWebAuthnCredential(
        callingActivity: Activity,
        options: AuthAssociateWebAuthnCredentialsOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.associateWebAuthnCredential().execute(callingActivity, options) }

    override fun listWebAuthnCredentials(
        onSuccess: Consumer<AuthListWebAuthnCredentialsResult>,
        onError: Consumer<AuthException>
    ) = listWebAuthnCredentials(AuthListWebAuthnCredentialsOptions.defaults(), onSuccess, onError)

    override fun listWebAuthnCredentials(
        options: AuthListWebAuthnCredentialsOptions,
        onSuccess: Consumer<AuthListWebAuthnCredentialsResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.listWebAuthnCredentials().execute(options) }

    override fun autoSignIn(onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { queueFacade.autoSignIn() }

    override fun deleteWebAuthnCredential(credentialId: String, onSuccess: Action, onError: Consumer<AuthException>) =
        deleteWebAuthnCredential(credentialId, AuthDeleteWebAuthnCredentialOptions.defaults(), onSuccess, onError)

    override fun deleteWebAuthnCredential(
        credentialId: String,
        options: AuthDeleteWebAuthnCredentialOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { useCaseFactory.deleteWebAuthnCredential().execute(credentialId, options) }

    override fun getEscapeHatch() = realPlugin.escapeHatch()

    override fun getPluginKey() = AWS_COGNITO_AUTH_PLUGIN_KEY

    override fun getVersion() = BuildConfig.VERSION_NAME

    /**
     * Federate to Identity Pool
     * @param providerToken Provider token to start the federation for
     * @param authProvider The auth provider you want to federate for (e.g. Facebook, Google, etc.)
     * @param onSuccess Success callback
     */
    fun federateToIdentityPool(
        providerToken: String,
        authProvider: AuthProvider,
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) {
        queueFacade.federateToIdentityPool(
            providerToken,
            authProvider,
            FederateToIdentityPoolOptions.builder().build()
        )
    }

    /**
     * Federate to Identity Pool
     * @param providerToken Provider token to start the federation for
     * @param authProvider The auth provider you want to federate for (e.g. Facebook, Google, etc.)
     * @param options Advanced options for federating to identity pool
     * @param onSuccess Success callback
     */
    fun federateToIdentityPool(
        providerToken: String,
        authProvider: AuthProvider,
        options: FederateToIdentityPoolOptions,
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) {
        queueFacade.federateToIdentityPool(
            providerToken,
            authProvider,
            options
        )
    }

    /**
     * Clear Federation to Identity Pool
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    fun clearFederationToIdentityPool(onSuccess: Action, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { queueFacade.clearFederationToIdentityPool() }

    fun fetchMFAPreference(onSuccess: Consumer<UserMFAPreference>, onError: Consumer<AuthException>) =
        enqueue(onSuccess, onError) { queueFacade.fetchMFAPreference() }

    @Deprecated("Use updateMFAPreference(sms, totp, email, onSuccess, onError) instead")
    fun updateMFAPreference(
        sms: MFAPreference?,
        totp: MFAPreference?,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.updateMFAPreference(sms, totp, null) }

    fun updateMFAPreference(
        sms: MFAPreference? = null,
        totp: MFAPreference? = null,
        email: MFAPreference? = null,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) = enqueue(onSuccess, onError) { queueFacade.updateMFAPreference(sms, totp, email) }

    private fun enqueue(onSuccess: Action, onError: Consumer<AuthException>, block: suspend () -> Unit) =
        enqueue({ onSuccess.call() }, onError::accept, block)

    private fun <T : Any> enqueue(onSuccess: Consumer<T>, onError: Consumer<AuthException>, block: suspend () -> T) =
        enqueue(onSuccess::accept, onError::accept, block)

    /**
     * Enqueue block to run sequentially with other blocks. Results are passed to onSuccess and any thrown exceptions
     * are passed to onError
     */
    private fun <T : Any> enqueue(onSuccess: (T) -> Unit, onError: (AuthException) -> Unit, block: suspend () -> T) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = block()
                    pluginScope.launch { onSuccess(result) }
                } catch (e: Exception) {
                    pluginScope.launch { onError(e.toAuthException()) }
                }
            }
        )
    }

    private fun throwIt(e: Throwable): Unit = throw e
}
