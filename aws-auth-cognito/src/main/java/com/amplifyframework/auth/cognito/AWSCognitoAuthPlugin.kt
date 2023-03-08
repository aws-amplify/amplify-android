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
import androidx.annotation.RestrictTo
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
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.UnknownException
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

    private val logger =
        Amplify.Logging.forNamespace(AWS_COGNITO_AUTH_LOG_NAMESPACE.format(this::class.java.simpleName))

    @VisibleForTesting
    internal lateinit var realPlugin: RealAWSCognitoAuthPlugin

    private val pluginScope = CoroutineScope(Job() + Dispatchers.Default)
    private val queueFacade: KotlinAuthFacadeInternal by lazy {
        KotlinAuthFacadeInternal(realPlugin)
    }

    private val queueChannel = Channel<Job>(capacity = Channel.UNLIMITED).apply {
        pluginScope.launch {
            consumeEach { it.join() }
        }
    }

    private lateinit var pluginConfigurationJSON: JSONObject

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getPluginConfiguration(): JSONObject {
        return pluginConfigurationJSON
    }

    private fun Exception.toAuthException(): AuthException {
        return if (this is AuthException) {
            this
        } else {
            UnknownException(cause = this)
        }
    }

    override fun initialize(context: Context) {
        realPlugin.initialize()
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        pluginConfigurationJSON = pluginConfiguration
        try {
            val configuration = AuthConfiguration.fromJson(pluginConfiguration)
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
        } catch (exception: Exception) {
            throw ConfigurationException(
                "Failed to configure AWSCognitoAuthPlugin.",
                "Make sure your amplifyconfiguration.json is valid.",
                exception
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
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signUp(username, password, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.confirmSignUp(username, confirmationCode)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.confirmSignUp(username, confirmationCode, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun resendSignUpCode(
        username: String,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.resendSignUpCode(username)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.resendSignUpCode(username, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signIn(
        username: String?,
        password: String?,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signIn(username, password)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signIn(username, password, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmSignIn(
        challengeResponse: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.confirmSignIn(challengeResponse)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.confirmSignIn(challengeResponse, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signInWithSocialWebUI(provider, callingActivity)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signInWithSocialWebUI(provider, callingActivity, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signInWithWebUI(callingActivity)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.signInWithWebUI(callingActivity, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun handleWebUISignInResponse(intent: Intent?) {
        queueFacade.handleWebUISignInResponse(intent)
    }

    override fun fetchAuthSession(
        options: AuthFetchSessionOptions,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.fetchAuthSession(options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.fetchAuthSession()
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.rememberDevice()
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.forgetDevice()
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun forgetDevice(
        device: AuthDevice,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.forgetDevice(device)
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun fetchDevices(
        onSuccess: Consumer<List<AuthDevice>>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.fetchDevices()
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.resetPassword(username, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun resetPassword(
        username: String,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.resetPassword(username)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.confirmResetPassword(username, newPassword, confirmationCode, options)
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.confirmResetPassword(username, newPassword, confirmationCode)
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.updatePassword(oldPassword, newPassword)
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun fetchUserAttributes(
        onSuccess: Consumer<List<AuthUserAttribute>>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.fetchUserAttributes()
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.updateUserAttribute(attribute, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.updateUserAttribute(attribute)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions,
        onSuccess: Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.updateUserAttributes(attributes, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        onSuccess: Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.updateUserAttributes(attributes)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.resendUserAttributeConfirmationCode(attributeKey, options)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.resendUserAttributeConfirmationCode(attributeKey)
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.confirmUserAttribute(attributeKey, confirmationCode)
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun getCurrentUser(
        onSuccess: Consumer<AuthUser>,
        onError: Consumer<AuthException>
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.getCurrentUser()
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

    override fun signOut(onComplete: Consumer<AuthSignOutResult>) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                val result = queueFacade.signOut()
                onComplete.accept(result)
            }
        )
    }

    override fun signOut(options: AuthSignOutOptions, onComplete: Consumer<AuthSignOutResult>) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                val result = queueFacade.signOut(options)
                onComplete.accept(result)
            }
        )
    }

    override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.deleteUser()
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }

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
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.federateToIdentityPool(
                        providerToken,
                        authProvider,
                        FederateToIdentityPoolOptions.builder().build()
                    )
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
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
    ) {
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val result = queueFacade.federateToIdentityPool(
                        providerToken,
                        authProvider,
                        options
                    )
                    onSuccess.accept(result)
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
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
        queueChannel.trySend(
            pluginScope.launch(start = CoroutineStart.LAZY) {
                try {
                    queueFacade.clearFederationToIdentityPool()
                    onSuccess.call()
                } catch (e: Exception) {
                    onError.accept(e.toAuthException())
                }
            }
        )
    }
}
