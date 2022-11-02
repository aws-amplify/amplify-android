@file:Suppress("IMPLICIT_CAST_TO_ANY")

package featureTest.utilities

import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.resetPassword
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signIn
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signUp
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Factory to create specialized options object for the top level APIs
 */
object AuthOptionsFactory {
    @Suppress("UNCHECKED_CAST")
    fun <T> create(apiName: AuthAPI, optionsData: JsonObject): T = when (apiName) {
        AuthAPI.confirmResetPassword -> AuthConfirmResetPasswordOptions.defaults()
        resetPassword -> AuthResetPasswordOptions.defaults()
        signUp -> getSignUpOptions(optionsData)
        AuthAPI.confirmSignIn -> AuthConfirmSignInOptions.defaults()
        AuthAPI.confirmSignUp -> AuthConfirmSignUpOptions.defaults()
        AuthAPI.confirmUserAttribute -> null
        AuthAPI.deleteUser -> null
        AuthAPI.fetchAuthSession -> getFetchAuthSessionOptions(optionsData)
        AuthAPI.fetchDevices -> null
        AuthAPI.fetchUserAttributes -> TODO()
        AuthAPI.forgetDevice -> TODO()
        AuthAPI.getCurrentUser -> TODO()
        AuthAPI.handleWebUISignInResponse -> TODO()
        AuthAPI.rememberDevice -> TODO()
        AuthAPI.resendSignUpCode -> AuthResendSignUpCodeOptions.defaults()
        AuthAPI.resendUserAttributeConfirmationCode -> AuthResendUserAttributeConfirmationCodeOptions.defaults()
        signIn -> AuthSignInOptions.defaults()
        AuthAPI.signInWithSocialWebUI -> AuthWebUISignInOptions.builder().build()
        AuthAPI.signInWithWebUI -> AuthWebUISignInOptions.builder().build()
        AuthAPI.signOut -> getSignOutOptions(optionsData)
        AuthAPI.updatePassword -> TODO()
        AuthAPI.updateUserAttribute -> AuthUpdateUserAttributeOptions.defaults()
        AuthAPI.updateUserAttributes -> AuthUpdateUserAttributesOptions.defaults()
        AuthAPI.clearFederationToIdentityPool -> TODO()
        AuthAPI.configure -> TODO()
        AuthAPI.federateToIdentityPool -> TODO()
        AuthAPI.getEscapeHatch -> TODO()
        AuthAPI.getPluginKey -> TODO()
        AuthAPI.getVersion -> TODO()
    } as T

    private fun getSignUpOptions(optionsData: JsonObject): AuthSignUpOptions =
        AuthSignUpOptions.builder().userAttributes(
            (optionsData["userAttributes"] as Map<String, String>).map {
                AuthUserAttribute(AuthUserAttributeKey.custom(it.key), (it.value as JsonPrimitive).content)
            }
        ).build()

    private fun getSignOutOptions(optionsData: JsonObject): AuthSignOutOptions {
        val globalSignOutData = (optionsData["globalSignOut"] as JsonPrimitive).booleanOrNull ?: false
        return AuthSignOutOptions.builder()
            .globalSignOut(globalSignOutData)
            .build()
    }

    private fun getFetchAuthSessionOptions(optionsData: JsonObject): AuthFetchSessionOptions {
        val refresh = (optionsData["forceRefresh"] as? JsonPrimitive)?.booleanOrNull ?: false
        return AuthFetchSessionOptions.builder()
            .forceRefresh(refresh)
            .build()
    }
}
