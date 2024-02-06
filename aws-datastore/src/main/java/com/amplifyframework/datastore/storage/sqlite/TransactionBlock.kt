package com.amplifyframework.datastore.storage.sqlite

import com.amplifyframework.datastore.DataStoreException

fun interface TransactionBlock {
    /**
     * Call to begin the block.
     */
    @Throws(DataStoreException::class)
    fun run()
}
