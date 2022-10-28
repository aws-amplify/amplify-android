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

import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.clearFederationToIdentityPool
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.configure
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.confirmResetPassword
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.confirmSignIn
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.confirmSignUp
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.confirmUserAttribute
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.deleteUser
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.federateToIdentityPool
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.fetchAuthSession
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.fetchDevices
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.fetchUserAttributes
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.forgetDevice
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.getCurrentUser
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.getEscapeHatch
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.getPluginKey
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.getVersion
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.handleWebUISignInResponse
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.rememberDevice
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.resendSignUpCode
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.resendUserAttributeConfirmationCode
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.resetPassword
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signIn
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signInWithSocialWebUI
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signInWithWebUI
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signOut
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.signUp
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.updatePassword
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.updateUserAttribute
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.updateUserAttributes
import com.amplifyframework.auth.cognito.featuretest.AuthAPI.values
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.google.gson.Gson
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NewAPI(internal val method: (AWSCognitoAuthPlugin, Map<String, *>, JsonObject) -> Unit)

object APIExecutor {
    private lateinit var latch: CountDownLatch
    private var result: Any? = null

    fun execute(sut: AWSCognitoAuthPlugin, apiName: AuthAPI, namedParams: Map<String, *>, options: JsonObject): Any? {
        latch = CountDownLatch(1)
        apis[apiName]?.method?.invoke(sut, namedParams, options)
        latch.await(5, TimeUnit.SECONDS)
        return result
    }

    /**
     * Generated code
     *
     * AWSCognitoAuthPlugin::class.declaredFunctions
     * .forEach {
     *     print("AuthAPI.${it.name} ->")
     *     print(" sut.${it.name}(")
     *     val params = it.parameters.filter { param ->
     *         param.kind == KParameter.Kind.VALUE
     *     }.joinToString(separator = ", ") { param ->
     *         if (param.type.classifier as KClass<*> == Action::class) {
     *             "getAction()"
     *         } else if (param.name == "options") {
     *             "AuthOptionsFactory.create(authAPI, options)"
     *         } else if (param.name in listOf("onSuccess", "onError", "onComplete")) {
     *             "getConsumer()"
     *         } else {
     *             "getParam(\"${param.name}\", namedParams)"
     *         }
     *     }
     *
     *     println("$params)")
     * }
    */
    private val apis = values().associateWith { authAPI ->
        NewAPI { sut, namedParams, options ->
            when (authAPI) {
                clearFederationToIdentityPool -> sut.clearFederationToIdentityPool(getAction(), getConsumer())
                configure -> sut.configure(
                    getParam("pluginConfiguration", namedParams),
                    getParam("context", namedParams)
                )
                confirmResetPassword -> sut.confirmResetPassword(
                    getParam("username", namedParams),
                    getParam("newPassword", namedParams),
                    getParam("confirmationCode", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getAction(),
                    getConsumer()
                )
                confirmSignIn -> sut.confirmSignIn(
                    getParam("challengeResponse", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                confirmSignUp -> sut.confirmSignUp(
                    getParam("username", namedParams),
                    getParam("confirmationCode", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                confirmUserAttribute -> sut.confirmUserAttribute(
                    getParam("attributeKey", namedParams),
                    getParam("confirmationCode", namedParams),
                    getAction(),
                    getConsumer()
                )
                deleteUser -> sut.deleteUser(getAction(), getConsumer())
                federateToIdentityPool -> sut.federateToIdentityPool(
                    getParam("providerToken", namedParams),
                    getParam("authProvider", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                fetchAuthSession -> sut.fetchAuthSession(
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                fetchDevices -> sut.fetchDevices(getConsumer(), getConsumer())
                fetchUserAttributes -> sut.fetchUserAttributes(getConsumer(), getConsumer())
                forgetDevice -> sut.forgetDevice(getParam("device", namedParams), getAction(), getConsumer())
                getCurrentUser -> sut.getCurrentUser(getConsumer(), getConsumer())
                getEscapeHatch -> sut.escapeHatch
                getPluginKey -> sut.pluginKey
                getVersion -> sut.version
                handleWebUISignInResponse -> sut.handleWebUISignInResponse(getParam("intent", namedParams))
                rememberDevice -> sut.rememberDevice(getAction(), getConsumer())
                resendSignUpCode -> sut.resendSignUpCode(
                    getParam("username", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                resendUserAttributeConfirmationCode -> sut.resendUserAttributeConfirmationCode(
                    getParam(
                        "attributeKey",
                        namedParams
                    ),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                resetPassword -> sut.resetPassword(
                    getParam("username", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                signIn -> sut.signIn(
                    getParam("username", namedParams),
                    getParam("password", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                signInWithSocialWebUI -> sut.signInWithSocialWebUI(
                    getParam("provider", namedParams),
                    getParam("callingActivity", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                signInWithWebUI -> sut.signInWithWebUI(
                    getParam("callingActivity", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                signOut -> sut.signOut(AuthOptionsFactory.create(authAPI, options), getConsumer())
                signUp -> sut.signUp(
                    getParam("username", namedParams),
                    getParam("password", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                updatePassword -> sut.updatePassword(
                    getParam("oldPassword", namedParams),
                    getParam("newPassword", namedParams),
                    getAction(),
                    getConsumer()
                )
                updateUserAttribute -> sut.updateUserAttribute(
                    getParam("attribute", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
                updateUserAttributes -> sut.updateUserAttributes(
                    getParam("attributes", namedParams),
                    AuthOptionsFactory.create(authAPI, options),
                    getConsumer(),
                    getConsumer()
                )
            }
        }
    }

    /**
     * Traverses given json to find value of paramName
     */
    private inline fun <reified T> getParam(paramName: String, paramsObject: Map<String, *>): T {
        paramsObject.entries.first {
            it.key == paramName
        }.apply {
            return Gson().fromJson(value.toString(), T::class.java)
        }
    }

    private fun <T> getConsumer(): Consumer<T> = Consumer<T> {
        result = it
        latch.countDown()
    }

    private fun getAction(): Action = Action {
        result = Unit
        latch.countDown()
    }
}
