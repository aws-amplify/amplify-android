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

package com.amplifyframework.statemachine.util

import io.kotest.matchers.shouldBe
import org.junit.Test

class MaskUtilTest {

    @Test
    fun `masks string`() {
        val string = "hello world"
        string.mask() shouldBe "hell***"
    }

    @Test
    fun `string of length four is just stars`() {
        val string = "1234"
        string.mask() shouldBe "***"
    }

    @Test
    fun `string of length one is just stars`() {
        val string = "1"
        string.mask() shouldBe "***"
    }

    @Test
    fun `null string is just stars`() {
        val string: String? = null
        string.mask() shouldBe "***"
    }

    @Test
    fun `masks values in map`() {
        val map = mapOf("test" to "something", "other" to "otherthing", "short" to "123")
        map.mask("test", "short").toString() shouldBe "{test=some***, other=otherthing, short=***}"
    }
}
