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

import aws.sdk.kotlin.runtime.endpoint.functions.parseArn
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.util.length
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.RealAWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.google.gson.Gson
import featureTest.utilities.APICaptorFactory.Companion.onError


import generated.model.ApiCall
import generated.model.TypeResponse
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions



/**
 * Executes the API on given [AWSCognitoAuthPlugin] instance
 */
internal val apiExecutor: (AWSCognitoAuthPlugin, ApiCall, TypeResponse) -> Any = { authPlugin: AWSCognitoAuthPlugin, api: ApiCall, responseType : TypeResponse ->

    lateinit var result: Any
    val latch = CountDownLatch(1)
    val targetApis = authPlugin::class.declaredFunctions.filter { it.name == api.name }

    val mapped : Map<String, Document?> = api!!.params!!.asMap()
    var requiredParams: Map<KParameter, Any?>? = null
    var targetApi: KFunction<*>? = null
    println(api.name)


    for (currentApi in targetApis) {
        try {
            val currentParams = currentApi.parameters.associateWith { kParam ->


                when {
                    kParam.kind == KParameter.Kind.INSTANCE -> authPlugin
                    kParam.type.classifier as KClass<*> == Action::class -> Action {
                        result = Unit
                        latch.countDown()

                    }
                    kParam.type.classifier as KClass<*> == Consumer::class -> Consumer<Any> { value ->
                        result = value
                        latch.countDown()


                    }


                    kParam.name == "options" -> AuthOptionsFactory.create(AuthAPI.valueOf(api.name!!), api.options!!.asMap()!!)
                    else -> kParam.name?.let { getParam(it, kParam, mapped) }
                }
            }
            targetApi = currentApi
            requiredParams = currentParams
            break
        } catch (ex: Exception) {

            print(ex.toString())
        }
    }

    if (targetApi == null || requiredParams == null)
        throw Exception("No matching api function with required parameters found")


    targetApi.callBy(requiredParams)


    latch.await(5, TimeUnit.SECONDS)



    result
}

/**
 * Traverses given json to find value of paramName
 */
private inline fun getParam(paramName: String, kParam: KParameter, paramsObject: Map<String, Document?>): kotlin.Any {
    paramsObject.entries.first {
        it.key == paramName
    }.apply {


        return Gson().fromJson(value!!.asString(), (kParam.type.classifier as KClass<*>).javaObjectType)


    }
}