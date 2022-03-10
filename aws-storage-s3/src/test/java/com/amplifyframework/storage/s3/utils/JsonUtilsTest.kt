package com.amplifyframework.storage.s3.utils

import junit.framework.TestCase

class JsonUtilsTest : TestCase() {

    fun testMapToString() {
        val input = mapOf("key1" to "value1", "key2" to "value2")
        val expectedOutput = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        val output = JsonUtils.mapToString(input)
        assertEquals(output, expectedOutput)
    }

    fun testJsonToMap() {
        val input = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        val expectedOutput = mapOf("key1" to "value1", "key2" to "value2")
        val output = JsonUtils.jsonToMap(input)
        assertEquals(output, expectedOutput)
    }
}