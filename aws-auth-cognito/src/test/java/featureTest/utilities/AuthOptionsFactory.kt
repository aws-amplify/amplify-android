package featureTest.utilities

import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.resetPassword
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signIn
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signOut
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signUp
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Factory to create specialized options object for the top level APIs
 */
object AuthOptionsFactory {
    fun create(apiName: AuthAPI, optionsData: JsonObject): Any? = when (apiName) {
        resetPassword -> AuthResetPasswordOptions.defaults()
        signUp -> getSignUpOptions(optionsData)
        signIn -> AuthSignInOptions.defaults()
        signOut -> getSignOutOptions(optionsData)
        else -> throw Error("Options for $apiName is not defined!")
    }

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
}
