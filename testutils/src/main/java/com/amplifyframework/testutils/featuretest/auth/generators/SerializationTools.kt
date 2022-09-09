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
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import java.io.File
import java.io.FileWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val basePath = ".temp/feature-test/testsuites"

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

    val dirName = api.name.name
    val fileName = description.replace(" ", "_").plus(".json")
    writeFile(result, dirName, fileName)
    println("Json exported:\n $result")
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
        is CognitoIdentityException, is CognitoIdentityProviderException, is AuthException
        -> toExceptionJsonElement(this)
        else -> JsonPrimitive(toString())
    }
}

fun toExceptionJsonElement(exception: Any): JsonElement {
    val message = when(exception) {
        is CognitoIdentityProviderException -> exception.message
        is CognitoIdentityException -> exception.message
        is AuthException -> exception.message
        else -> null
    }
    val responseMap = mutableMapOf<String, Any?>(
        "errorType" to exception::class.simpleName,
        "errorMessage" to message
    )

    if (exception is AuthException) {
        responseMap["recoverySuggestion"] = exception.recoverySuggestion
        responseMap["cause"] = exception.cause
    }
    return responseMap.toJsonElement()
}
