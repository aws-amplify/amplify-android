package com.atlasv.android.amplify.simpleappsync.storage

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.storage.StorageItemChange
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG

/**
 * weiping@atlasv.com
 * 2022/12/7
 */
fun <T : Model> SQLiteStorageAdapter.deleteList(items: List<T>) {
    for (item in items) {
        if (!sqlQueryProcessor.modelExists(item, QueryPredicates.all())) {
            LOG.debug(item.modelName + " model with id = " + item.primaryKeyString + " does not exist.")
            continue
        }
        writeData(item, StorageItemChange.Type.DELETE)
        LOG.debug("Delete $item")
    }
}

fun <T : Model> SQLiteStorageAdapter.saveList(items: List<T>) {
    if (items.isEmpty()) {
        return
    }
    val startMs = System.currentTimeMillis()
    for (item in items) {
        val writeType: StorageItemChange.Type = if (sqlQueryProcessor.modelExists(item, QueryPredicates.all())) {
            StorageItemChange.Type.UPDATE
        } else {
            StorageItemChange.Type.CREATE
        }
        writeData(item, writeType)
    }
    LOG.info("Save ${items[0].modelName} list(${items.size}), ${(System.currentTimeMillis() - startMs)}ms")
}
