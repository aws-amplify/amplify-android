package com.amplifyframework.storage.s3.transfer

import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TransferDBTest {

    @After
    fun teardown() {
        // Clear instance
        TransferDB.instance = null
    }

    @Test
    fun `getInstance returns the same object`() {
        val context = RuntimeEnvironment.getApplication()

        val db1 = TransferDB.getInstance(context)
        val db2 = TransferDB.getInstance(context)

        db1 shouldBeSameInstanceAs db2
    }
}
