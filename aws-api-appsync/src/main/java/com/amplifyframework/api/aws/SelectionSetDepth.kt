/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.api.aws

/**
 * GraphQL request options that allows for configurable depth.
 */
class SelectionSetDepth(private val maxDepth: Int = 2) : GraphQLRequestOptions {

    override fun paginationFields(): List<String> {
        return listOf(NEXT_TOKEN_KEY)
    }

    override fun modelMetaFields(): List<String> {
        return emptyList()
    }

    override fun listField(): String {
        return ITEMS_KEY
    }

    override fun maxDepth(): Int {
        return maxDepth
    }

    override fun leafSerializationBehavior(): LeafSerializationBehavior {
        return LeafSerializationBehavior.ALL_FIELDS
    }

    companion object {
        private const val NEXT_TOKEN_KEY = "nextToken"
        private const val ITEMS_KEY = "items"

        @JvmStatic
        fun defaultDepth() = SelectionSetDepth()

        @JvmStatic
        fun onlyIncluded() = SelectionSetDepth(maxDepth = 0)
    }
}
