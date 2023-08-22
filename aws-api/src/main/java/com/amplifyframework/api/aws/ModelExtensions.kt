package com.amplifyframework.api.aws

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelIdentifier
import java.io.Serializable

fun Model.getSortedIdentifiers(): List<Serializable> {
    return when (val identifier = resolveIdentifier()) {
        is ModelIdentifier<*> -> { listOf(identifier.key()) + identifier.sortedKeys() }
        else -> listOf(identifier.toString())
    }
}
