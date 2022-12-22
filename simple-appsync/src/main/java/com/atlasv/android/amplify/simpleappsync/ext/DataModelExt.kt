package com.atlasv.android.amplify.simpleappsync.ext

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

/**
 * weiping@atlasv.com
 * 2022/12/22
 */
fun <T : Model> T.withMetadata(modelWithMetadata: ModelWithMetadata<T>?): ModelWithMetadata<T>? {
    modelWithMetadata ?: return null
    return ModelWithMetadata(this, modelWithMetadata.syncMetadata)
}

fun Model.resolveMethod(methodName: String): String? {
    return try {
        if (!this.javaClass.methods.any { it.name == methodName }) {
            return null
        }
        val method: Method = this.javaClass.getMethod(methodName)
        method.invoke(this)?.toString() ?: ""
    } catch (exception: Throwable) {
        null
    }
}

fun Model.setFieldByReflection(fieldName: String, value: String) {
    try {
        this.javaClass.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(this@setFieldByReflection, value)
        }
    } catch (exception: Throwable) {
        // no op
    }
}

fun Model.setFieldByReflection(fieldName: String, value: Int) {
    try {
        this.javaClass.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(this@setFieldByReflection, value)
        }
    } catch (exception: Throwable) {
        // no op
    }
}

var Model.itemDisplayName
    get():String? {
        return resolveMethod("getDisplayName")
    }
    set(value) {
        value ?: return
        setFieldByReflection("displayName", value)
    }

var Model.sort
    get():Int? {
        return resolveMethod("getSort")?.toIntOrNull()
    }
    set(value) {
        value ?: return
        setFieldByReflection("sort", value)
    }

val Model.itemName
    get():String? {
        return resolveMethod("getName")
    }

val Model.isLocaleMode get() = modelName.endsWith("Locale")

val Model.materialID
    get():String? {
        return resolveMethod(if (this.javaClass.simpleName == "VFXLocale") "getVfxId" else "getMaterialId")
    }

fun <T : Model> T.ensureDisplayName() {
    try {
        if (this.javaClass.methods.any { it.name == "getName" } && this.javaClass.methods.any { it.name == "getDisplayName" }) {
            if (this.itemDisplayName.isNullOrEmpty()) {
                this.itemDisplayName = this.itemName
            }
        }
    } catch (cause: Throwable) {
        //
    }
}

fun Date.simpleFormat(): String {
    return SimpleDateFormat.getDateTimeInstance().format(this)
}

fun <T : Model> Class<T>.queryField(fieldName: String): QueryField? {
    return declaredFields.find { it.name == fieldName }?.let {
        QueryField.field(fieldName)
    }
}

fun QueryPredicate.andIfNotNull(newPredicate: QueryPredicate?): QueryPredicate {
    newPredicate ?: return this
    return this.and(newPredicate)
}