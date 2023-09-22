package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.testmodels.lazy.AmplifyModelProvider
import com.amplifyframework.testmodels.lazy.Comment
import com.amplifyframework.testmodels.lazy.Post
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AWSApiSchemaRegistryTest {

    private val schemaRegistry = AWSApiSchemaRegistry()

    @Before
    fun setUp() {
        mockkStatic(ModelProviderLocator::class)
        every { ModelProviderLocator.locate() } returns AmplifyModelProvider.getInstance()
    }

    @After
    fun tearDown() {
        clearStaticMockk(ModelProviderLocator::class)
    }

    @Test
    fun models_are_registered() {
        assertNotNull(schemaRegistry.getModelSchemaForModelClass(Post::class.java))
        assertNotNull(schemaRegistry.getModelSchemaForModelClass("Post"))
        assertNotNull(schemaRegistry.getModelSchemaForModelClass(Comment::class.java))
    }

    @Test(expected = ApiException::class)
    fun models_not_provided_throw() {
        assertNotNull(schemaRegistry.getModelSchemaForModelClass(Todo::class.java))
    }
}
