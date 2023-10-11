/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.aws

import com.amplifyframework.testmodels.lazy.Blog
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiLoadedModelListTest {

    @Test
    fun loaded_list_provides_items() {
        val expectedItems = listOf<Blog>(
            Blog.builder().name("b1").build(),
            Blog.builder().name("b2").build()
        )

        val loadedList = ApiLoadedModelList(expectedItems)

        assertEquals(expectedItems, loadedList.items)
    }
}
