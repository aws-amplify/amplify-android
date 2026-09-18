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
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.testmodels.lazy.Post
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Tests the reflective search for the code-generated model provider.
 *
 * Every case here is a way the class path can differ from what codegen is expected to have produced,
 * and the point of each assertion is that the failure arrives as a configuration error naming the class
 * rather than as a reflection exception.
 */
class AppSyncModelProviderLocatorTest {

    @Test
    fun `locates a provider and returns its instance`() {
        val provider = AppSyncModelProviderLocator.locate(
            "com.amplifyframework.testmodels.lazy.AmplifyModelProvider"
        )

        provider.models() shouldContain Post::class.java
    }

    @Test
    fun `a missing provider class is a configuration error naming the class`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            AppSyncModelProviderLocator.locate("com.example.NotHere")
        }

        error.message shouldContain "com.example.NotHere"
        error.cause.shouldBeInstanceOf<ClassNotFoundException>()
    }

    @Test
    fun `a class that is not a model provider is a configuration error`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            AppSyncModelProviderLocator.locate("java.lang.String")
        }

        error.message shouldContain "does not implement"
    }

    @Test
    fun `a provider with no getInstance method is a configuration error`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            AppSyncModelProviderLocator.locate(NoAccessor::class.java.name)
        }

        error.message shouldContain "getInstance"
    }

    @Test
    fun `a non-static getInstance is a configuration error rather than a null receiver failure`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            AppSyncModelProviderLocator.locate(InstanceAccessor::class.java.name)
        }

        error.message shouldContain "not static"
    }

    @Test
    fun `an exception thrown out of getInstance is reported as unknown, not as misconfiguration`() {
        // The provider is exactly where it should be and has the right shape, so nothing about the
        // configuration is wrong — the generated code itself failed.
        val error = shouldThrow<AppSyncUnknownException> {
            AppSyncModelProviderLocator.locate(ThrowingAccessor::class.java.name)
        }

        error.cause.shouldBeInstanceOf<IllegalStateException>().message shouldBe "generated code failed"
    }

    @Test
    fun `a getInstance that returns nothing is a configuration error`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            AppSyncModelProviderLocator.locate(NullAccessor::class.java.name)
        }

        error.message shouldContain "returned no model provider"
    }

    // ── Providers with the shapes the cases above look for ──────────────

    class NoAccessor : ModelProvider {
        override fun models(): Set<Class<out Model>> = emptySet()
        override fun version() = "1"
    }

    class InstanceAccessor : ModelProvider {
        override fun models(): Set<Class<out Model>> = emptySet()
        override fun version() = "1"

        @Suppress("unused")
        fun getInstance(): ModelProvider = this
    }

    class ThrowingAccessor : ModelProvider {
        override fun models(): Set<Class<out Model>> = emptySet()
        override fun version() = "1"

        companion object {
            @JvmStatic
            @Suppress("unused")
            fun getInstance(): ModelProvider = throw IllegalStateException("generated code failed")
        }
    }

    class NullAccessor : ModelProvider {
        override fun models(): Set<Class<out Model>> = emptySet()
        override fun version() = "1"

        companion object {
            @JvmStatic
            @Suppress("unused")
            fun getInstance(): ModelProvider? = null
        }
    }
}
