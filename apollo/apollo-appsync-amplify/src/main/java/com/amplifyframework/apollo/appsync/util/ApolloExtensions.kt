/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.apollo.appsync.util

import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpMethod.GET as SmithyGet
import aws.smithy.kotlin.runtime.http.HttpMethod.POST as SmithyPost
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod

internal fun HttpMethod.toSmithyMethod() = when (this) {
    HttpMethod.Get -> SmithyGet
    HttpMethod.Post -> SmithyPost
}

internal fun List<HttpHeader>.toSmithyHeaders() = Headers {
    for (header in this@toSmithyHeaders) {
        append(header.name, header.value)
    }
}
