package com.atlasv.android.amplify.simpleappsync

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.ModelWithMetadata

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifyModelMerger(private val sqliteStorage: AmplifySqliteStorage) {
    fun <T : Model> mergeAll(modelWithMetadataList: Iterable<ModelWithMetadata<T>?>) {
        sqliteStorage.use { adapter ->

            adapter.saveList(modelWithMetadataList.mapNotNull {
                it?.takeIf { !it.isDeleted }?.model
            })

            adapter.deleteList(modelWithMetadataList.mapNotNull {
                it?.takeIf { it.isDeleted }?.model
            })
        }
    }
}