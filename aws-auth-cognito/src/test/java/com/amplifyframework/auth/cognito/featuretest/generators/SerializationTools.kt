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

package com.amplifyframework.auth.cognito.featuretest.generators

import aws.sdk.kotlin.services.cognitoidentity.model.CognitoIdentityException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.serializers.CognitoIdentityExceptionSerializer
import com.amplifyframework.auth.cognito.featuretest.serializers.CognitoIdentityProviderExceptionSerializer
import com.amplifyframework.auth.cognito.featuretest.serializers.deserializeToAuthState
import com.amplifyframework.auth.cognito.featuretest.serializers.serialize
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

const val basePath = "aws-auth-cognito/src/test/resources/feature-test"

fun writeFile(json: String, dirName: String, fileName: String) {
    val directory = File("$basePath/$dirName")
    directory.mkdirs()
    val filePath = "${directory.path}/$fileName"

    val fileWriter = FileWriter(filePath)
    fileWriter.write(json)
    fileWriter.close()
    println("File written in ${directory.absolutePath}")
}

fun cleanDirectory() {
    val directory = File(basePath)
    if (directory.exists()) {
        directory.deleteRecursively()
    }
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
 * Generates a md file with all the test cases formatted.
 */
internal fun List<FeatureTestCase>.exportToMd() {
    val outputStream = FileOutputStream("testSuite.md")
    val writer = outputStream.bufferedWriter()
    val jsonFormat = Json { prettyPrint = true }

    groupBy { it.api.name }
        .forEach {
            with(writer) {
                newLine()
                write("# ${it.key.name}") // Header 1
                newLine()
                it.value.forEach {
                    newLine()
                    write("## Case: *${it.description}*")
                    newLine()
                    // Preconditions (GIVEN)
                    write("### Preconditions") // Header 2
                    newLine()
                    write("- **Amplify Configuration**: ${it.preConditions.`amplify-configuration`}")
                    newLine()
                    write("- **Initial State:** ${it.preConditions.state}")
                    newLine()
                    write("- **Mock Responses:** ")
                    printCodeBlock {
                        if (it.preConditions.mockedResponses.isEmpty()) "[]" else
                            jsonFormat.encodeToString(it.preConditions.mockedResponses)
                    }

                    // Parameters (WHEN)
                    write("### Input")
                    newLine()
                    write("- **params:**")
                    printCodeBlock {
                        jsonFormat.encodeToString(it.api.params)
                    }
                    write("- **options:**")
                    printCodeBlock { jsonFormat.encodeToString(it.api.options) }

                    // Then
                    write("### Validations")
                    newLine()
                    it.validations.forEach {
                        printCodeBlock { jsonFormat.encodeToString(it) }
                    }
                }
            }
        }
    writer.flush()
}

private fun BufferedWriter.printCodeBlock(blob: () -> String) {
    newLine()
    write("```json")
    newLine()
    write(blob.invoke())
    newLine()
    write("```")
    newLine()
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
        is Instant -> JsonPrimitive(this.epochSeconds)
        is AuthException -> toJsonElement()
        is AWSCognitoAuthSignInOptions -> toJsonElement()
        is CognitoIdentityProviderException -> Json.encodeToJsonElement(
            CognitoIdentityProviderExceptionSerializer,
            this
        )
        is AuthSessionResult<*> -> toJsonElement()
        is CognitoIdentityException -> Json.encodeToJsonElement(CognitoIdentityExceptionSerializer, this)
        else -> gsonBasedSerializer(this)
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

fun AuthSessionResult<*>.toJsonElement(): JsonElement {
    return (if (type == AuthSessionResult.Type.SUCCESS) value else error).toJsonElement()
}

/**
 * Uses Gson to convert objects which cannot be serialized,
 * tries to convert to map of params to vals
 */
fun gsonBasedSerializer(value: Any): JsonElement {
    val gson = Gson()
    return try {
        gson.fromJson(gson.toJson(value).toString(), Map::class.java).toJsonElement()
    } catch (ex: Exception) {
        reflectionBasedSerializer(value)
    }
}

/**
 * Final fallback to serialize by using reflection, traversing the object members and converting it to Map.
 * Note that this method is similar to what Gson does. But Gson fails when there is name collision in parent and child
 * classes.
 */
fun reflectionBasedSerializer(value: Any): JsonElement {
    return (value::class as KClass<*>).declaredMemberProperties.filter {
        it.visibility == KVisibility.PUBLIC
    }.associate {
        it.name to it.getter.call(value)
    }.toMap().toJsonElement()
}