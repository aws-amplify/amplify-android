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
