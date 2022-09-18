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

package com.amplifyframework.testutils.featuretest.auth.generators

import aws.sdk.kotlin.services.cognitoidentity.model.CognitoIdentityException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import com.amplifyframework.testutils.featuretest.auth.serializers.CognitoIdentityExceptionSerializer
import com.amplifyframework.testutils.featuretest.auth.serializers.CognitoIdentityProviderExceptionSerializer
import com.amplifyframework.testutils.featuretest.auth.serializers.deserializeToAuthState
import com.amplifyframework.testutils.featuretest.auth.serializers.serialize
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.FileWriter

const val basePath = ".temp/feature-test"

fun writeFile(json: String, dirName: String, fileName: String) {
    val directory = File("$basePath/$dirName")
    directory.mkdirs()
    val filePath = "${directory.path}/$fileName"

    val fileWriter = FileWriter(filePath)
    fileWriter.write(json)
    fileWriter.close()
    println("File written in ${directory.absolutePath}")
}

internal fun FeatureTestCase.exportJson() {
    val format = Json {
        prettyPrint = true
    }

    val result = format.encodeToString(this)

    val dirName = "testsuites/" + api.name.name
    val fileName = description.replace(" ", "_").plus(".json")
    writeFile(result, dirName, fileName)
    println("Json exported:\n $result")
}

internal fun AuthState.exportJson() {
    val result = this.serialize()
    val reverse = result.deserializeToAuthState()

    val dirName = "states"
    val fileName = "${authNState?.javaClass?.simpleName}_${authZState?.javaClass?.simpleName}.json"
    writeFile(result, dirName, fileName)
    println("Json exported:\n $result")
    println("Serialized can be reversed = ${reverse.serialize() == result}")
}

/**
 * Extension class to convert primitives and collections
 * from [https://github.com/Kotlin/kotlinx.serialization/issues/296#issuecomment-1132714147]
 */
fun Map<*, *>.toJsonElement(): JsonElement {
    return JsonObject(
        mapNotNull {
            (it.key as? String ?: return@mapNotNull null) to it.value.toJsonElement()
        }.toMap()
    )
}

fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is Map<*, *> -> toJsonElement()
        is Collection<*> -> toJsonElement()
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is AuthException -> toJsonElement()
        is CognitoIdentityProviderException -> Json.encodeToJsonElement(
            CognitoIdentityProviderExceptionSerializer,
            this
        )
        is CognitoIdentityException -> Json.encodeToJsonElement(CognitoIdentityExceptionSerializer, this)
        else -> JsonPrimitive(toString())
    }
}

fun AuthException.toJsonElement(): JsonElement {
    val responseMap = mutableMapOf<String, Any?>(
        "errorType" to this::class.simpleName,
        "errorMessage" to message,
        "recoverySuggestion" to recoverySuggestion,
        "cause" to cause
    )

    return responseMap.toJsonElement()
}
