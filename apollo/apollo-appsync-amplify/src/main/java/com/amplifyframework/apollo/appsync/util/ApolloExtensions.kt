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
