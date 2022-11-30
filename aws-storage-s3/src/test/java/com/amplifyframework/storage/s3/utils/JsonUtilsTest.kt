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

package com.amplifyframework.storage.s3.utils

import junit.framework.TestCase

class JsonUtilsTest : TestCase() {

    fun testMapToString() {
        val input = mapOf("key1" to "value1", "key2" to "value2")
        val expectedOutput = "{\"key1\":\"value1\",\"key2\":\"value2\"}"
        val output = JsonUtils.mapToString(input)
        assertEquals(output, expectedOutput)
    }

    fun testJsonToMap() {
        val input = "{\"key1\":\"value1\",\"key2\":\"value2\"}"
        val expectedOutput = mapOf("key1" to "value1", "key2" to "value2")
        val output = JsonUtils.jsonToMap(input)
        assertEquals(output, expectedOutput)
    }
}
