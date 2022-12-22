package com.atlasv.android.amplifysyncsample

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.generated.model.*
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent
import com.atlasv.android.appcontext.AppContextHolder

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
object AmplifyHelper {
    private val dataStoreConfiguration by lazy {
        DataStoreConfiguration.builder()
            .syncPageSize(Int.MAX_VALUE)
            //查询版本号不高于本地引擎版本号（所有功能都应向前兼容）的资源
            .syncExpression(VFX::class.java) {
                QueryOptionFactory.vfxQueryCondition
            }
            .syncExpression(VideoFilter::class.java) {
                VideoFilter.VFX_ENGINE_MIN_VERSION_CODE.le(2)
            }
            .syncExpression(FontVFX::class.java) {
                FontVFX.VFX_ENGINE_MIN_VERSION_CODE.le(2)
            }
            .syncExpression(TextTemplate::class.java) {
                TextTemplate.TARGET_VERSION_CODE.le(3)
            }
            .syncExpression(TransitionVFX::class.java) {
                TransitionVFX.VFX_ENGINE_MIN_VERSION_CODE.le(2)
            }
            .syncExpression(ClipAnim::class.java) {
                ClipAnim.TARGET_VERSION_CODE.le(1)
            }
            .build()
    }

    val syncExcludeModels by lazy {
        setOf(
            Recommend::class.java,
            RecommendCategory::class.java,
            FilterVFX::class.java,
            FilterVFXCategory::class.java,
            FontVFX::class.java,
            FontVFXCategory::class.java,
        )
    }
    val modelProvider by lazy {
        object : ModelProvider {
            override fun models(): MutableSet<Class<out Model>> {
                return AmplifyModelProvider.getInstance().models()
                    .filterNot {
                        it in syncExcludeModels || it.simpleName.startsWith("MS")
                    }.toMutableSet()
            }

            override fun version(): String {
                return AmplifyModelProvider.getInstance().version()
            }
        }
    }

    val component by lazy {
        AmplifySimpleSyncComponent(
            AppContextHolder.appContext,
            dataStoreConfiguration,
            modelProvider,
            SchemaRegistry.instance()
        )
    }
}