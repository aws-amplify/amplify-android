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
package com.amplifyframework.storage.s3.transfer

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.readAll

internal open class ProgressListenerInterceptor(
    private val progressListener: ProgressListener
) : HttpInterceptor {
    fun convertBodyWithProgressUpdates(httpBody: HttpBody): HttpBody {
        return when (httpBody) {
            is HttpBody.ChannelContent -> {
                SdkByteReadChannelWithProgressUpdates(
                    httpBody,
                    progressListener
                )
            }
            is HttpBody.SourceContent -> {
                SourceContentWithProgressUpdates(
                    httpBody,
                    progressListener
                )
            }
            is HttpBody.Bytes -> {
                httpBody
            }
            is HttpBody.Empty -> {
                httpBody
            }
        }
    }

    internal class SourceContentWithProgressUpdates(
        private val sourceContent: HttpBody.SourceContent,
        private val progressListener: ProgressListener
    ) : HttpBody.SourceContent() {
        private val delegate = sourceContent.readFrom()
        override val contentLength: Long?
            get() = sourceContent.contentLength

        override fun readFrom(): SdkSource {
            return object : SdkSource {
                override fun close() {
                    delegate.close()
                }

                override fun read(sink: SdkBuffer, limit: Long): Long {
                    return delegate.read(sink, limit).also {
                        if (it > 0) {
                            progressListener.progressChanged(it)
                        }
                    }
                }
            }
        }
    }

    internal class SdkByteReadChannelWithProgressUpdates(
        private val httpBody: ChannelContent,
        private val progressListener: ProgressListener
    ) : HttpBody.ChannelContent() {
        val delegate = httpBody.readFrom()
        override val contentLength: Long?
            get() = httpBody.contentLength
        override fun readFrom(): SdkByteReadChannel {
            return object : SdkByteReadChannel by delegate {
                override val availableForRead: Int
                    get() = delegate.availableForRead

                override val isClosedForRead: Boolean
                    get() = delegate.isClosedForRead

                override val isClosedForWrite: Boolean
                    get() = delegate.isClosedForWrite

                override fun cancel(cause: Throwable?): Boolean {
                    return delegate.cancel(cause)
                }

                override suspend fun read(sink: SdkBuffer, limit: Long): Long {
                    return delegate.readAll(sink).also {
                        if (it > 0) {
                            progressListener.progressChanged(it)
                        }
                    }
                }
            }
        }
    }
}

internal class DownloadProgressListenerInterceptor(
    private val progressListener: ProgressListener
) : ProgressListenerInterceptor(progressListener) {
    override suspend fun modifyBeforeDeserialization(
        context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>
    ): HttpResponse {
        val body = convertBodyWithProgressUpdates(context.protocolResponse.body)
        return HttpResponse(context.protocolResponse.status, context.protocolResponse.headers, body)
    }
}

internal class UploadProgressListenerInterceptor(
    private val progressListener: ProgressListener
) : ProgressListenerInterceptor(progressListener) {
    override suspend fun modifyBeforeTransmit(
        context: ProtocolRequestInterceptorContext<Any, HttpRequest>
    ): HttpRequest {
        val builder = context.protocolRequest.toBuilder()
        builder.body = convertBodyWithProgressUpdates(builder.body)
        return builder.build()
    }
}
