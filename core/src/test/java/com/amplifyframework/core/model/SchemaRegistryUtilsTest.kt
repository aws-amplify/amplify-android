package com.amplifyframework.core.model

import com.amplifyframework.datastore.DataStoreException.IrRecoverableException
import com.amplifyframework.testmodels.lazy.Post
import com.amplifyframework.testmodels.phonecall.Phone
import com.amplifyframework.testmodels.todo.Todo
import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaRegistryUtilsTest {

    @Test(expected = IrRecoverableException::class)
    fun throws_if_has_lazy_detected() {
        SchemaRegistryUtils.registerSchema(
            mutableMapOf(),
            "Post",
            ModelSchema.fromModelClass(Post::class.java)
        )
    }

    @Test
    fun test_register() {
        val schemaMap = mutableMapOf<String, ModelSchema>()
        val expectedKey = "Todo"
        val expectedValue = ModelSchema.fromModelClass(Todo::class.java)

        SchemaRegistryUtils.registerSchema(
            schemaMap,
            expectedKey,
            expectedValue
        )

        assertEquals(1, schemaMap.size)
        assertEquals(expectedValue, schemaMap[expectedKey])
    }

    @Test
    fun test_registers() {
        val schemaMap = mutableMapOf<String, ModelSchema>()
        val expectedKey = "Todo"
        val expectedValue = ModelSchema.fromModelClass(Todo::class.java)

        SchemaRegistryUtils.registerSchemas(
            schemaMap,
            mapOf((expectedKey to expectedValue), ("TodoOwner" to ModelSchema.fromModelClass(Phone::class.java)))
        )

        assertEquals(2, schemaMap.size)
        assertEquals(expectedValue, schemaMap[expectedKey])
    }

    @Test
    fun test_empty_schemas() {
        val schemaMap = mutableMapOf<String, ModelSchema>()

        SchemaRegistryUtils.registerSchemas(schemaMap)

        assertEquals(0, schemaMap.size)
    }

    @Test
    fun test_schema_missing_class_catches_exception_and_continues() {
        val schemaMap = mutableMapOf<String, ModelSchema>()

        SchemaRegistryUtils.registerSchema(schemaMap, "Empty", ModelSchema.builder().name("Empty").build())

        assertEquals(1, schemaMap.size)
    }
}