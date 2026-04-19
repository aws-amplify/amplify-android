/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
