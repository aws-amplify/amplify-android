/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model

import com.amplifyframework.testmodels.lazy.Comment
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadedModelReferenceImplTest {

    @Test
    fun model_reference_provides_value() {
        val expectedComment = Comment.builder().text("Hello").post(mockk()).build()
        val loadedModelReference = LoadedModelReferenceImpl(expectedComment)

        assertEquals(expectedComment, loadedModelReference.value)
        assertEquals(0, loadedModelReference.getIdentifier().size)
    }

    @Test
    fun null_reference_provides_null_value() {
        val loadedModelReference = LoadedModelReferenceImpl(null)

        assertNull(loadedModelReference.value)
        assertEquals(0, loadedModelReference.getIdentifier().size)
    }
}
