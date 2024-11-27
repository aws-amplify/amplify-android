/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.util

import aws.smithy.kotlin.runtime.content.Document
import com.amplifyframework.annotations.InternalAmplifyApi
import java.lang.StringBuilder
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts a Smithy document to a JSON string
 */
@InternalAmplifyApi
fun Document.toJsonString(): String = buildString { appendTo(this) }

/**
 * Converts a JSON string to a Smithy document.
 * NOTE: This assumes the string represents a JSON object!
 */
@Suppress("FunctionName")
@InternalAmplifyApi
fun JsonDocument(content: String): Document = DocumentBuilder().process(JSONObject(content))

private fun Document?.appendTo(builder: StringBuilder) {
    when (val doc = this) {
        is Document.String -> {
            builder.append('"')
            builder.append(doc.asString())
            builder.append('"')
        }
        is Document.Boolean -> builder.append(doc.asBoolean())
        is Document.List -> {
            builder.append('[')
            doc.forEachIndexed { index, document ->
                document.appendTo(builder)
                if (index < doc.size - 1) builder.append(',')
            }
            builder.append(']')
        }
        is Document.Map -> {
            builder.append('{')
            doc.entries.forEachIndexed { index, (key, value) ->
                builder.append('"')
                builder.append(key)
                builder.append("\":")
                value.appendTo(builder)
                if (index < doc.size - 1) builder.append(',')
            }
            builder.append('}')
        }
        is Document.Number -> builder.append(doc.value)
        null -> builder.append("null")
    }
}

internal class DocumentBuilder {
    fun process(obj: JSONObject): Document.Map {
        val map = mutableMapOf<String, Document?>()
        obj.keys().forEach { key ->
            val document = process(obj.get(key))
            map[key] = document
        }
        return Document.Map(map)
    }

    fun process(array: JSONArray): Document.List {
        val list = mutableListOf<Document?>()
        for (i in 0 until array.length()) {
            val document = process(array.opt(i))
            list.add(document)
        }
        return Document.List(list)
    }

    fun process(value: Any?): Document? = when (value) {
        is JSONArray -> process(value)
        is JSONObject -> process(value)
        is Number -> Document(value)
        is String -> Document(value)
        is Boolean -> Document(value)
        JSONObject.NULL -> null
        null -> null
        else -> throw IllegalArgumentException("Unknown value type")
    }
}
