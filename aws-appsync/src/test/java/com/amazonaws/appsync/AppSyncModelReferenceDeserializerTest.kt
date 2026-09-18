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

import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelReference
import com.amplifyframework.testmodels.lazy.AmplifyModelProvider
import com.amplifyframework.testmodels.lazy.Blog
import com.google.gson.reflect.TypeToken
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the related-model deserializer, through [AppSyncGson] rather than in isolation, so the
 * registration is covered too — an adapter that is written correctly but never registered would pass any
 * test that instantiated it directly.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncModelReferenceDeserializerTest {

    private val loader = RecordingModelLoader { Blog.builder().name("My Blog").build() }

    private val gson = AppSyncGson(
        loader = loader,
        schemaRegistry = AppSyncSchemaRegistry { AmplifyModelProvider.getInstance() }
    ).gson

    private val blogReferenceType = object : TypeToken<ModelReference<Blog>>() {}.type

    @Test
    fun `a reference carrying more than the key is already loaded`() {
        val reference: ModelReference<Blog> = gson.fromJson(
            """{"id":"b1","name":"My Blog"}""",
            blogReferenceType
        )

        reference.shouldBeInstanceOf<LoadedModelReference<Blog>>().value?.name shouldBe "My Blog"
    }

    @Test
    fun `a reference carrying only the key is lazy, and queries on access`() = runTest {
        val reference: ModelReference<Blog> = gson.fromJson("""{"id":"b1"}""", blogReferenceType)

        val lazy = reference.shouldBeInstanceOf<AppSyncLazyModelReference<Blog>>()
        lazy.getIdentifier() shouldContainExactly mapOf("id" to "b1")
        loader.calls shouldBe 0

        lazy.fetchModel()?.name shouldBe "My Blog"
        loader.requests.single().variables shouldContainExactly mapOf("id" to "b1")
    }

    @Test
    fun `an incomplete key resolves to null rather than querying with a key it does not have`() = runTest {
        val reference: ModelReference<Blog> = gson.fromJson("""{"name":"My Blog"}""", blogReferenceType)

        reference.shouldBeInstanceOf<AppSyncLazyModelReference<Blog>>().fetchModel().shouldBeNull()

        loader.calls shouldBe 0
    }

    @Test
    fun `a null relationship is a null field, so no reference is produced`() {
        val reference: ModelReference<Blog>? = gson.fromJson("null", blogReferenceType)

        reference.shouldBeNull()
    }

    @Test
    fun `a reference requested without a model type is reported rather than silently lazy`() {
        val error = shouldThrow<AppSyncDeserializationException> {
            gson.fromJson<ModelReference<*>>("""{"id":"b1"}""", ModelReference::class.java)
        }

        error.message shouldContain "model type"
    }

    @Test
    fun `a model type the provider does not know is reported as a configuration error`() {
        val error = shouldThrow<AppSyncInvalidConfigException> {
            gson.fromJson<ModelReference<NotGenerated>>(
                """{"id":"b1"}""",
                object : TypeToken<ModelReference<NotGenerated>>() {}.type
            )
        }

        error.message shouldContain "NotGenerated"
    }

    private class NotGenerated : Model
}
