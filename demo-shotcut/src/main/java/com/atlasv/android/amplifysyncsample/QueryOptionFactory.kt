package com.atlasv.android.amplifysyncsample

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.generated.model.Font2
import com.amplifyframework.datastore.generated.model.VFX
import com.amplifyframework.datastore.generated.model.VFXLocale

/**
 * weiping@atlasv.com
 * 2022/6/14
 */
object QueryOptionFactory {
    private const val LOCAL_BASE = "base"

    val vfxQueryCondition: QueryPredicate
        get() = VFX.VFX_ENGINE_MIN_VERSION_CODE.le(6)
            .and(VFX.LANG_CODE.eq(LOCAL_BASE))
    val vfxQueryOption: QueryOptions get() = Where.matches(vfxQueryCondition)

    val fontQueryCondition: QueryOptions
        get() = Where.matches(Font2.ONLINE.eq(1)).sorted(QueryField.field("sort").ascending())

    val vfxLocaleCondition: QueryPredicate get() = buildLocaleQueryCondition(VFXLocale::class.java.simpleName)
    val vfxLocaleQueryOption: QueryOptions get() = Where.matches(vfxLocaleCondition)

    fun buildLocaleQueryCondition(
        modelName: String
    ): QueryPredicate {
        val matchField = QueryField.field(modelName, "locale")
        return matchField.eq("en")
    }
}

fun <T : Model> Class<T>.getSimpleLocaleQueryPredicate(): QueryPredicate {
    return QueryOptionFactory.buildLocaleQueryCondition(this.simpleName)
}

fun <T : Model> Class<T>.getSimpleLocaleQueryOptions(): QueryOptions {
    return Where.matches(getSimpleLocaleQueryPredicate())
}