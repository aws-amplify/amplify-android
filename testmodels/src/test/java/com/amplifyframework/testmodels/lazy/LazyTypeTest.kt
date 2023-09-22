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
                ?.hasLazySupport ?: false
        )
    }
}
