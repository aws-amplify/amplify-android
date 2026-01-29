package com.amplifyframework.core.store

import io.kotest.matchers.equals.shouldBeEqual
import java.util.UUID
import org.junit.Test

class InMemoryKeyValueRepositoryProviderTest {

    @Test
    fun `test ability to create multiple in memory repositories`() {
        val expectedRepo1Name = UUID.randomUUID().toString()
        val expectedRepo2Name = UUID.randomUUID().toString()
        val key1 = "testKey"
        val expectedRepo1Key1Value = "testVal1"
        val expectedRepo2Key1Value = "testVal2"
        val repo1 = InMemoryKeyValueRepositoryProvider.getKeyValueRepository(expectedRepo1Name).apply {
            put(key1, expectedRepo1Key1Value)
        }
        val repo2 = InMemoryKeyValueRepositoryProvider.getKeyValueRepository(expectedRepo2Name).apply {
            put(key1, expectedRepo2Key1Value)
        }

        expectedRepo1Key1Value shouldBeEqual repo1.get(key1)!!
        expectedRepo2Key1Value shouldBeEqual repo2.get(key1)!!
    }

    @Test
    fun `test ability to get repository by name`() {
        // GIVEN
        val expectedRepo1Name = UUID.randomUUID().toString()
        val key1 = "testKey"
        val expectedRepo1Key1Value = "testVal1"

        // WHEN
        val repo1 = InMemoryKeyValueRepositoryProvider.getKeyValueRepository(expectedRepo1Name).apply {
            put(key1, expectedRepo1Key1Value)
        }

        // THEN
        expectedRepo1Key1Value shouldBeEqual repo1.get(key1)!!

        // WHEN
        val repo2 = InMemoryKeyValueRepositoryProvider.getKeyValueRepository(expectedRepo1Name)

        // THEN
        expectedRepo1Key1Value shouldBeEqual repo2.get(key1)!!
    }
}
