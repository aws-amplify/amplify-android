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

package com.amplifyframework.testmodels.lazy

import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.testmodels.todo.Todo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyTypeTest {

    @Test
    fun check_lazy_support() {
        assertTrue(
            ModelSchema.fromModelClass(Post::class.java)
                .modelClass.getAnnotation(ModelConfig::class.java)
                ?.hasLazySupport ?: false
        )
    }

    @Test
    fun check_older_model_no_lazy_support() {
        assertFalse(
            ModelSchema.fromModelClass(Todo::class.java)
                .modelClass.getAnnotation(ModelConfig::class.java)
                ?.hasLazySupport ?: true
        )
    }
}
