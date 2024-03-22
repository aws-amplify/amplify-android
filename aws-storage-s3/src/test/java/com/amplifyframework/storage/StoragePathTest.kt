package com.amplifyframework.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class StoragePathTest {

    @Test
    fun `string storage path`() {
        val expectedString = "storage/path"

        val path = StoragePath.fromString(expectedString) as StringStoragePath

        assertEquals(expectedString, path.resolvePath())
    }

    @Test
    fun `identity id storage path`() {
        val expectedString = "/photos/123/1.jpg"

        val path = StoragePath.fromIdentityId { "photos/$it/1.jpg" } as IdentityIdProvidedStoragePath

        assertEquals(expectedString, path.resolvePath("123"))
    }
}
