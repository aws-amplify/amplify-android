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

package featureTest.utilities

import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import aws.smithy.kotlin.runtime.content.Document
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.resetPassword
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signIn
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signUp
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
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
import io.mockk.InternalPlatformDsl.toStr
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.json.JSONObject

/**
 * Factory to create specialized options object for the top level APIs
 */
object AuthOptionsFactory {
    fun <T> create(apiName: AuthAPI, optionsData : Map<String, Document?>): T = when (apiName) {
        AuthAPI.confirmResetPassword -> AuthConfirmResetPasswordOptions.defaults()
        resetPassword -> AuthResetPasswordOptions.defaults()
        signUp -> getSignUpOptions(optionsData)//AuthSignUpOptions.builder().build()getSignUpOptions(optionsData) //userAttribute(AuthUserAttributeKey.email(), "{email}").build()//.builder().build()//getSignUpOptions(optionsData)
        AuthAPI.confirmSignIn -> AuthConfirmSignInOptions.defaults()
        AuthAPI.confirmSignUp -> AuthConfirmSignUpOptions.defaults()
        AuthAPI.confirmUserAttribute -> null
        AuthAPI.deleteUser -> null
        AuthAPI.fetchAuthSession -> getFetchAuthSessionOptions(optionsData)
        AuthAPI.fetchDevices -> null
        AuthAPI.fetchUserAttributes -> TODO()
        AuthAPI.forgetDevice -> null
        AuthAPI.getCurrentUser -> null
        AuthAPI.handleWebUISignInResponse -> TODO()
        AuthAPI.rememberDevice -> null
        AuthAPI.resendSignUpCode -> AuthResendSignUpCodeOptions.defaults()
        AuthAPI.resendUserAttributeConfirmationCode -> AuthResendUserAttributeConfirmationCodeOptions.defaults()
        signIn -> getSignInOptions(optionsData)
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
        else ->TODO()
    } as T

    private fun getSignInOptions(optionsData: Map<String, Document?>): AuthSignInOptions {
        var option =
            AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.CUSTOM_AUTH_WITH_SRP).build()

        return option


    }

    private fun getSignUpOptions(optionsData: Map<String, Document?>) : AuthSignUpOptions {
        var option =
            AuthSignUpOptions.builder().userAttribute(AuthUserAttributeKey.email(), JSONObject(optionsData["userAttributes"]!!.asString())["email"].toStr()).build()
        return option

    }
        /*AuthSignUpOptions.builder().userAttributes(
            (optionsData["userAttributes"] as Map<String, Document?>).map {
                AuthUserAttribute(AuthUserAttributeKey.custom(it.key), it.value!!.asString())
            }
        ).build()*/


    private fun getSignOutOptions(optionsData: Map<String, Document?>): AuthSignOutOptions {
        val globalSignOutData = (optionsData["globalSignOut"] as JsonPrimitive).booleanOrNull ?: false
        return AuthSignOutOptions.builder()
            .globalSignOut(globalSignOutData)
            .build()
    }

    private fun getFetchAuthSessionOptions(optionsData: Map<String, Document?>): AuthFetchSessionOptions {
        val refresh = (optionsData["forceRefresh"] as? JsonPrimitive)?.booleanOrNull ?: false
        return AuthFetchSessionOptions.builder()
            .forceRefresh(refresh)
            .build()
    }


}
