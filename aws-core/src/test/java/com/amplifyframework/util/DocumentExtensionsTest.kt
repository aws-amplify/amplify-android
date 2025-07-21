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
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentExtensionsTest {
    @Test
    fun `converts number`() {
        val doc = Document(42.0)
        doc.toJsonString() shouldBe "42.0"
    }

    @Test
    fun `converts int`() {
        val doc = Document(42)
        doc.toJsonString() shouldBe "42"
    }

    @Test
    fun `converts boolean`() {
        val falseDoc = Document(false)
        val trueDoc = Document(true)
        falseDoc.toJsonString() shouldBe "false"
        trueDoc.toJsonString() shouldBe "true"
    }

    @Test
    fun `converts string`() {
        val doc = Document("hello world")
        doc.toJsonString() shouldBe "\"hello world\""
    }

    @Test
    fun `converts list`() {
        val list = listOf(
            Document("a"),
            Document(2),
            null,
            Document(true)
        )
        val doc = Document(list)
        doc.toJsonString() shouldBe """
            ["a",2,null,true]
        """.trimIndent()
    }

    @Test
    fun `converts map`() {
        val map = mapOf(
            "a" to Document(1),
            "b" to Document("b"),
            "c" to null,
            "d" to Document(true)
        )
        val doc = Document(map)
        doc.toJsonString() shouldBe """
            {"a":1,"b":"b","c":null,"d":true}
        """.trimIndent()
    }

    @Test
    fun `converts complex document`() {
        val doc = Document(
            mapOf(
                "a" to Document(1),
                "b" to Document("b"),
                "c" to null,
                "d" to Document(listOf(Document("d1"), Document("d2"))),
                "e" to Document(
                    mapOf(
                        "e1" to Document(true),
                        "e2" to Document(listOf(Document("e2.1"), null)),
                        "e3" to Document(mapOf()),
                        "e4" to null
                    )
                )
            )
        )

        doc.toJsonString() shouldBe """
            {"a":1,"b":"b","c":null,"d":["d1","d2"],"e":{"e1":true,"e2":["e2.1",null],"e3":{},"e4":null}}
        """.trimIndent()
    }

    @Test
    fun `converts complex json into document and back again`() {
        val json = """
            {"a":1,"b":"b","c":null,"d":["d1","d2"],"e":{"e1":true,"e2":["e2.1",null],"e3":{},"e4":null}}
        """.trimIndent()

        val doc = JsonDocument(json)
        val result = doc.toJsonString()

        result shouldBe json
    }
}
