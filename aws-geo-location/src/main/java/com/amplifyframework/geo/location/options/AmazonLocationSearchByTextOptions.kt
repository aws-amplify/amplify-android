/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.location.options

import com.amplifyframework.geo.options.GeoSearchByTextOptions

class AmazonLocationSearchByTextOptions private constructor(
    val searchIndex: String? = null,
    builder: Builder
) : GeoSearchByTextOptions(builder) {
    companion object {
        @JvmStatic fun builder() = Builder()

        @JvmStatic fun defaults() = builder().build()
    }

    class Builder : GeoSearchByTextOptions.Builder() {
        var searchIndex: String? = null
            private set
        fun searchIndex(searchIndex: String) = apply { this.searchIndex = searchIndex }
        override fun build() = AmazonLocationSearchByTextOptions(searchIndex, this)
    }
}
