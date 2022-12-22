package com.atlasv.android.amplifysyncsample

import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.generated.model.VFX

/**
 * weiping@atlasv.com
 * 2022/6/14
 */
object QueryOptionFactory {
    private const val LOCAL_BASE = "base"
    val vfxQueryCondition: QueryPredicate
        get() = VFX.VFX_ENGINE_MIN_VERSION_CODE.le(6)
            .and(VFX.LANG_CODE.eq(LOCAL_BASE))
}

