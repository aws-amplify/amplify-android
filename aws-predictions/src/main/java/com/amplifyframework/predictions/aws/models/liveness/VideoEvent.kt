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
package com.amplifyframework.predictions.aws.models.liveness

import android.util.Base64
import java.nio.ByteBuffer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
internal data class VideoEvent(
    @Serializable(with = ByteBufferSerializer::class) @SerialName("VideoChunk") val videoChunk: ByteBuffer,
    @SerialName("TimestampMillis") val timestampMillis: Long
)

private object ByteBufferSerializer : KSerializer<ByteBuffer> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteBuffer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ByteBuffer) {
        value.mark()
        val bytes = ByteArray(value.remaining())
        value.get(bytes, 0, bytes.size)
        value.reset()
        encoder.encodeString(Base64.encodeToString(bytes, Base64.NO_WRAP))
    }
    override fun deserialize(decoder: Decoder): ByteBuffer {
        val bytes = Base64.decode(decoder.decodeString(), Base64.NO_WRAP)
        return ByteBuffer.wrap(bytes)
    }
}
