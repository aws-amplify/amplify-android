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
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.google.gson.Gson
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions
import kotlinx.serialization.json.JsonObject

/**
 * Executes the API on given [AWSCognitoAuthPlugin] instance
 */
internal val apiExecutor: (AWSCognitoAuthPlugin, API) -> Any = { authPlugin: AWSCognitoAuthPlugin, api: API ->

    var result: Any = Unit
    val latch = CountDownLatch(1)
    val targetApis = authPlugin::class.declaredFunctions.filter { it.name == api.name.name }

    var requiredParams: Map<KParameter, Any?>? = null
    var targetApi: KFunction<*>? = null
    for (currentApi in targetApis) {
        try {
            // If we are attempting to call an api with options, ignore same named api without options
            if ((api.options as JsonObject).isNotEmpty() &&
                (currentApi.parameters.find { it.name == "options" } == null)
            ) {
                continue
            }
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
                    kParam.name == "options" -> AuthOptionsFactory.create(api.name, api.options as JsonObject)
                    else -> kParam.name?.let { getParam(it, kParam, api.params as JsonObject) }
                }
            }
            targetApi = currentApi
            requiredParams = currentParams
            break
        } catch (ex: Exception) {
            print(ex.toString())
        }
    }

    if (targetApi == null || requiredParams == null) {
        throw Exception("No matching api function with required parameters found")
    }
    targetApi.callBy(requiredParams)

    val complete = latch.await(15, TimeUnit.SECONDS)
    if (!complete) {
        throw Exception("Test did not invoke completion handlers within the allotted timeout")
    }
    result
}

/**
 * Traverses given json to find value of paramName
 */
private inline fun getParam(paramName: String, kParam: KParameter, paramsObject: Map<String, *>): kotlin.Any {
    paramsObject.entries.first {
        it.key == paramName
    }.apply {
        return Gson().fromJson(value.toString(), (kParam.type.classifier as KClass<*>).javaObjectType)
    }
}
