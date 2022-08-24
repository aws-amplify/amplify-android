package com.amplifyframework.testutils.featuretest.auth.generators

import com.amplifyframework.testutils.featuretest.FeatureTestCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.FileWriter

const val basePath = ".temp/feature-test/testsuites"


fun writeFile(json: String, dirName:String, fileName: String) {
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
        else -> JsonPrimitive(toString())
    }
}
