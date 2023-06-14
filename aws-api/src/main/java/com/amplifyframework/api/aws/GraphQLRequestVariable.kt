package com.amplifyframework.api.aws

/**
 * Holds information needed for a single variable for model querying
 */
internal data class GraphQLRequestVariable(val key: String, val value: Any, val type: String)
