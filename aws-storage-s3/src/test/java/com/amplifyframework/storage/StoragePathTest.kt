/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
        val expectedString = "photos/123/1.jpg"

        val path = StoragePath.fromIdentityId { "photos/$it/1.jpg" } as IdentityIdProvidedStoragePath

        assertEquals(expectedString, path.resolvePath("123"))
    }
}
