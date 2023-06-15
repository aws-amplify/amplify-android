package com.amplifyframework.core.model

import com.amplifyframework.testmodels.cpk.Blog.BlogIdentifier
import com.amplifyframework.testmodels.cpk.IntModelWithIdentifier.IntModelWithIdentifierIdentifier
import com.amplifyframework.testmodels.cpk.IntModelWithoutIdentifier
import com.amplifyframework.testmodels.cpk.StringModelWithIdentifier.StringModelWithIdentifierIdentifier
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelIdentifierTest {

    /**
     * Non Strings are expected to have additional quotes encapsulating the identifier
     */
    @Test
    fun cpk_non_string_model_identifier_with_no_sort_key() {
        val expectedKey = 123
        val expectedKeyString = "\"123\""

        val identifier = IntModelWithIdentifierIdentifier(expectedKey)

        assertEquals(expectedKeyString, identifier.identifier)
    }

    /**
     * Non Strings are expected to have additional quotes encapsulating the identifier
     * This example uses CPK + No sort key without ModelIdentifier
     */
    @Test
    fun cpk_non_string_without_model_identifier_with_no_sort_key() {
        val expectedKey = 123
        val expectedKeyString = "\"123\""

        val identifier = IntModelWithoutIdentifier.builder().customId(expectedKey).build().primaryKeyString

        assertEquals(expectedKeyString, identifier)
    }

    /**
     * Strings are not expected to have additional quotes encapsulating the identifier
     */
    @Test
    fun cpk_string_model_identifier_with_no_sort_key() {
        val expectedKey = "hello"

        val identifier = StringModelWithIdentifierIdentifier(expectedKey)


        assertEquals(expectedKey, identifier.identifier)
    }

    /**
     * CPK with Sort Keys are expected to have additional quotes encapsulating the identifier
     */
    @Test
    fun cpk_string_model_identifier_and_sort_keys() {
        val blogKey = "blog"
        val siteKey = "site"
        val expectedIdentifier = "\"blog\"#\"site\""

        val identifier = BlogIdentifier(blogKey, siteKey)


        assertEquals(expectedIdentifier, identifier.identifier)
    }
}