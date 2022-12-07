package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.kotlin.core.Amplify

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySimpleSyncComponent(
    private val appContext: Context,
    private val dataStoreConfiguration: DataStoreConfiguration,
    private val modelProvider: ModelProvider,
    private val schemaRegistry: SchemaRegistry,
    private val lastSync: Long,
    private val mergeListFactory: MergeRequestFactory = DefaultMergeRequestFactory
) {

    val storage by lazy {
        AmplifySqliteStorage(appContext, dataStoreConfiguration, modelProvider, schemaRegistry)
    }

    val merger by lazy {
        AmplifyModelMerger(storage)
    }

    suspend fun syncFromRemote() {
        try {
            val request = mergeListFactory.create(
                appContext, dataStoreConfiguration, modelProvider, schemaRegistry, lastSync
            )
            Amplify.API.query(request).data.forEach {
                merger.mergeAll(it.data.items)
            }
            LOG.info("syncFromRemote success")
        } catch (cause: Throwable) {
            LOG.error("syncFromRemote error", cause)
        }
    }

    companion object {
        val LOG = Amplify.Logging.forNamespace("amplify:simple-sync")
    }
}