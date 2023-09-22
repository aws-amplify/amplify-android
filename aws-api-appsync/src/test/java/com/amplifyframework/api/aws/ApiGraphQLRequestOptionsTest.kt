package com.amplifyframework.api.aws

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiGraphQLRequestOptionsTest {
    @Test
    fun testDefaultMaxDepth() {
        val options = ApiGraphQLRequestOptions()
        assertEquals(2, options.maxDepth())
    }

    @Test
    fun testCustomMaxDepth() {
        val customDepth = 1
        val options = ApiGraphQLRequestOptions(customDepth)
        assertEquals(customDepth, options.maxDepth())
    }
}
