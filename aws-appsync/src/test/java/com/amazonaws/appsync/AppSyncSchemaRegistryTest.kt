/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.core.model.Model
import com.amplifyframework.testmodels.lazy.AmplifyModelProvider
import com.amplifyframework.testmodels.lazy.Post
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

/**
 * Tests the model schema lookup a lazy relationship depends on for its primary key field names.
 */
class AppSyncSchemaRegistryTest {

    private var providerLookups = 0
    private val registry = AppSyncSchemaRegistry {
        providerLookups++
        AmplifyModelProvider.getInstance()
    }

    @Test
    fun `finds the schema for a model class`() {
        registry.schemaFor(Post::class.java).primaryIndexFields shouldContainExactly listOf("id")
    }

    @Test
    fun `an unknown model type is reported as a configuration error`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            registry.schemaFor(NotGenerated::class.java)
        }

        error.message shouldContain "NotGenerated"
    }

    @Test
    fun `the provider is not looked up until a schema is asked for`() {
        // The lookup is a reflective search of the class path, so a consumer whose responses carry no
        // lazily loaded relationships must never pay for it.
        providerLookups shouldBe 0

        registry.schemaFor(Post::class.java)
        registry.schemaFor(Post::class.java)

        providerLookups shouldBe 1
    }

    private class NotGenerated : Model
}
