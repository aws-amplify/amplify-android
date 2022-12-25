package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.kotlin.core.Amplify
import com.atlasv.android.amplify.simpleappsync.ext.*
import com.atlasv.android.amplify.simpleappsync.response.AmplifyModelMerger
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import java.util.*

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySimpleSyncComponent(
    private val appContext: Context,
    private val dataStoreConfiguration: DataStoreConfiguration,
    private val modelProvider: ModelProvider,
    private val schemaRegistry: SchemaRegistry,
    private val mergeListFactory: MergeRequestFactory = DefaultMergeRequestFactory,
    private val modelPreSaveAction: (Model) -> Unit = {}
) {

    val storage by lazy {
        AmplifySqliteStorage(appContext, dataStoreConfiguration, object : ModelProvider {
            override fun models(): MutableSet<Class<out Model>> {
                return modelProvider.models().filterNot {
                    it.simpleName.endsWith("Locale")
                }.toMutableSet()
            }

            override fun version(): String {
                return modelProvider.version()
            }
        }, schemaRegistry)
    }

    val merger by lazy {
        AmplifyModelMerger(storage, modelPreSaveAction)
    }

    suspend fun syncFromRemote(
        grayRelease: Int, dbInitTime: Long, locale: String
    ) {
        try {
            val lastSync = getLastSyncTime(dbInitTime)
            val lastLocale = AmplifyExtSettings.getLastModelLocale(appContext)
            val rebuildLocale = lastLocale != locale
            LOG.info("Start sync, lastLocale=$lastLocale, target locale=$locale, lastSync=${Date(lastSync).simpleFormat()}")
            val request = mergeListFactory.create(
                appContext,
                dataStoreConfiguration,
                modelProvider,
                schemaRegistry,
                lastSync,
                grayRelease,
                locale,
                rebuildLocale
            )
            val responseItemGroups = Amplify.API.query(request).data.map {
                it.data.items.toList()
            }
            val newestSyncTime = responseItemGroups.maxOfOrNull { list ->
                list.maxOfOrNull {
                    it.syncMetadata.lastChangedAt?.secondsSinceEpoch ?: 0L
                } ?: 0L
            }?.takeIf { it > lastSync }?.also {
                LOG.info("newestSyncTime=$it")
            }
            for (group in responseItemGroups) {
                if (group.firstOrNull()?.model?.isLocaleMode == true) {
                    continue
                }
                merger.mergeAll(group)
            }

            for (group in responseItemGroups) {
                applyLocaleModels(group).also {
                    merger.mergeAll(it)
                }
            }
            AmplifyExtSettings.saveLastSync(appContext, modelProvider.version(), newestSyncTime, locale)
            LOG.info(
                "syncFromRemote success, locale=$locale, date=${Date(newestSyncTime ?: lastSync).simpleFormat()}"
            )
        } catch (cause: Throwable) {
            LOG.error("syncFromRemote error", cause)
        }
    }

    private suspend fun getLastSyncTime(dbInitTime: Long): Long {
        if (modelProvider.version() != AmplifyExtSettings.getLastSyncDbVersion(appContext)) {
            return dbInitTime
        }
        return maxOf(dbInitTime, AmplifyExtSettings.getLastSyncTimestamp(appContext))
    }

    private fun applyLocaleModels(localeModels: List<ModelWithMetadata<Model>>): List<ModelWithMetadata<Model>> {
        val firstLocaleModel = localeModels.firstOrNull()?.model ?: return emptyList()
        if (!firstLocaleModel.isLocaleMode) {
            return emptyList()
        }
        val modelClass = modelProvider.models().find {
            it.simpleName == firstLocaleModel.modelName.removeSuffix("Locale")
        } ?: return emptyList()
        val modifiedModels = arrayListOf<ModelWithMetadata<Model>>()
        for (localeModel in localeModels) {
            if (localeModel.isDeleted) {
                continue
            }
            val materialModel = storage.query(
                modelClass, Where.matches(QueryField.field("id").eq(localeModel.model.materialID))
            )?.firstOrNull() ?: continue
            localizeModel(materialModel, localeModel.model)
            modifiedModels.add(
                ModelWithMetadata(
                    materialModel, ModelMetadata(
                        materialModel.modelName + "|" + materialModel.primaryKeyString,
                        false,
                        localeModel.syncMetadata.version,
                        localeModel.syncMetadata.lastChangedAt
                    )
                )
            )
        }
        return modifiedModels
    }

    private fun localizeModel(model: Model, localeModel: Model) {
        model.sort = localeModel.sort
        model.itemDisplayName = localeModel.itemName?.takeIf { it.isNotEmpty() } ?: model.itemName
    }

    companion object {
        val LOG = Amplify.Logging.forNamespace("amplify:simple-sync")
    }
}