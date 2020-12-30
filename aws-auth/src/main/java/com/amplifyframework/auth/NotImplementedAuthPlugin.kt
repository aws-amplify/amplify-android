package com.amplifyframework.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.amplifyframework.auth.options.AuthSignInOptions

import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer

import org.json.JSONObject

/**
 * Since I'm building a prototype, I'm not implementing all of the contracts.
 * I use this base class to reduce boiler plate in my PoC plugin implementation,
 * to help show which methods are important, and which ones are not. What's below is *not*.
 */
abstract class NotImplementedAuthPlugin<T>: AuthPlugin<T>() {
    // These methods must be implemented by the PoC.
    abstract override fun getPluginKey(): String
    abstract override fun configure(pluginConfiguration: JSONObject?, context: Context)
    abstract override fun getEscapeHatch(): T
    abstract override fun getVersion(): String

    // sign-in and sign-up methods are being shown in the PoC. Require them to be implemented.
    abstract override fun signUp(username: String, password: String, options: AuthSignUpOptions, onSuccess: Consumer<AuthSignUpResult>, onError: Consumer<AuthException>)
    abstract override fun confirmSignUp(username: String, confirmationCode: String, onSuccess: Consumer<AuthSignUpResult>, onError: Consumer<AuthException>)
    abstract override fun signIn(username: String?, password: String?, options: AuthSignInOptions, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>)

    // All other functionality is left to the complete implementation.

    override fun confirmSignIn(confirmationCode: String, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun signIn(username: String?, password: String?, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun resendSignUpCode(username: String, onSuccess: Consumer<AuthSignUpResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun signInWithSocialWebUI(provider: AuthProvider, callingActivity: Activity, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun signInWithSocialWebUI(provider: AuthProvider, callingActivity: Activity, options: AuthWebUISignInOptions, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun signInWithWebUI(callingActivity: Activity, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun signInWithWebUI(callingActivity: Activity, options: AuthWebUISignInOptions, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun handleWebUISignInResponse(intent: Intent?) {
        TODO("Not yet implemented")
    }

    override fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun forgetDevice(device: AuthDevice, onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun fetchDevices(onSuccess: Consumer<MutableList<AuthDevice>>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun resetPassword(username: String, onSuccess: Consumer<AuthResetPasswordResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun confirmResetPassword(newPassword: String, confirmationCode: String, onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun updatePassword(oldPassword: String, newPassword: String, onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun fetchUserAttributes(onSuccess: Consumer<MutableList<AuthUserAttribute>>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun updateUserAttribute(attribute: AuthUserAttribute, onSuccess: Consumer<AuthUpdateAttributeResult>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun updateUserAttributes(attributes: MutableList<AuthUserAttribute>, onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun resendUserAttributeConfirmationCode(attributeKey: AuthUserAttributeKey, onSuccess: Consumer<AuthCodeDeliveryDetails>, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun confirmUserAttribute(attributeKey: AuthUserAttributeKey, confirmationCode: String, onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun getCurrentUser(): AuthUser {
        TODO("Not yet implemented")
    }

    override fun signOut(onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun signOut(options: AuthSignOutOptions, onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }
}
