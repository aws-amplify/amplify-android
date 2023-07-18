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

package com.amplifyframework.predictions.aws.http

import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.predictions.aws.models.liveness.LivenessResponseStream
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.Date
import java.util.zip.CRC32
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

internal object LivenessEventStream {

    private val LOG = Amplify.Logging.logger(CategoryType.PREDICTIONS, "amplify:aws-predictions")

    fun encode(data: ByteArray, headers: Map<String, Any>): ByteBuffer {
        var headerLength = 0
        for (header in headers) {
            headerLength += header.key.length
            if (header.value is String) {
                headerLength += (header.value as String).length
                headerLength += 4 // 2 byte value length is still included
            } else if (header.value is Date) {
                headerLength += 8 // Header value length is 8 bytes
                headerLength += 2 // 2 byte value length is not included
            } else {
                // ByteArray
                headerLength += (header.value as ByteArray).size
                headerLength += 4 // 2 byte value length is still included
            }
        }
        val payloadLength = data.size
        val messageLength = 16 + payloadLength + headerLength
        val resultData = ByteBuffer.allocate(messageLength)

        resultData.putInt(messageLength)
        resultData.putInt(headerLength)

        val preludeData = Arrays.copyOfRange(resultData.array(), 0, 8)
        var crc = CRC32()
        crc.update(preludeData)
        var crcPrelude = crc.value.toInt()
        resultData.putInt(crcPrelude)

        for (header in headers) {
            val headerKeyLength = header.key.length.toUByte().toByte()

            resultData.put(headerKeyLength)
            val headerData = header.key.encodeUtf8().toByteArray()
            resultData.put(headerData)

            val headerType = if (header.value is String) {
                7
            } else if (header.value is Date) {
                8
            } else {
                6
            }
            resultData.put(headerType.toByte())

            if (header.value is String) {
                val headerValueLength = (header.value as String).length.toUShort().toShort()
                resultData.putShort(headerValueLength)
            } else if (header.value is ByteArray) {
                val headerValueLength = (header.value as ByteArray).size.toUShort().toShort()
                resultData.putShort(headerValueLength)
            }

            if (header.value is String) {
                val headerValueData = (header.value as String).encodeUtf8().toByteArray()
                resultData.put(headerValueData)
            } else if (header.value is Date) {
                val headerValueData = (header.value as Date).time
                resultData.putLong(headerValueData)
            } else {
                val headerValueData = header.value as ByteArray
                resultData.put(headerValueData)
            }
        }
        val payload = data
        resultData.put(payload)

        crc = CRC32()
        val messageData = Arrays.copyOfRange(resultData.array(), 0, messageLength - 4)
        crc.update(messageData)
        crcPrelude = crc.value.toInt()
        resultData.putInt(crcPrelude)
        return resultData.position(0) as ByteBuffer
    }

    fun decode(eventData: ByteString, json: Json): LivenessResponseStream? {
        if (eventData.size < 16) {
            LOG.error(
                "Error decoding liveness event stream data. Size should be at least 16 bytes, " +
                    "actual size is ${eventData.size}."
            )
            return null
        }
        val totalLength = eventData.substring(0, 4).toByteArray().toUInt32().toInt()
        if (eventData.size < totalLength) {
            LOG.error(
                "Error decoding liveness event stream data. Prelude specifies data size of $totalLength, " +
                    "actual size is ${eventData.size}."
            )
            return null
        }
        val headerLength = eventData.substring(4, 8).toByteArray().toUInt32().toInt()
        val headerData = eventData.substring(12, 12 + headerLength)
        var currentPosition = 0
        val headers = mutableMapOf<String, String>()
        while (currentPosition < headerLength) {
            val currentHeaderLength = headerData[currentPosition].toInt()
            currentPosition += 1
            val headerName =
                headerData.substring(currentPosition, currentPosition + currentHeaderLength).utf8()
            currentPosition += currentHeaderLength

            // Skip header type info, always 7 while decoding
            currentPosition += 1

            val currentHeaderValueLength =
                headerData.substring(currentPosition, currentPosition + 2).toByteArray().toUInt16().toInt()
            currentPosition += 2

            val headerValue =
                headerData.substring(currentPosition, currentPosition + currentHeaderValueLength).utf8()
            headers[headerName] = headerValue

            currentPosition += currentHeaderValueLength
        }

        val payloadStartPosition = 4 + 4 + 4 + headerLength
        val payloadLength = eventData.size - payloadStartPosition - 4
        val payloadString =
            eventData.substring(payloadStartPosition, payloadStartPosition + payloadLength).utf8()
        val jsonString = when {
            ":event-type" in headers.keys -> {
                "{\"${headers[":event-type"]}\":$payloadString}"
            }
            ":exception-type" in headers.keys -> {
                "{\"${headers[":exception-type"]}\":$payloadString}"
            }
            else -> {
                ""
            }
        }
        if (jsonString.isEmpty()) {
            LOG.error("Error deserializing liveness response.")
            return null
        }
        return json.decodeFromString<LivenessResponseStream>(jsonString)
    }

    private fun ByteArray.toUInt32(): UInt {
        return ((this[0].toUInt() and 0xFFu) shl 24) or ((this[1].toUInt() and 0xFFu) shl 16) or
            ((this[2].toUInt() and 0xFFu) shl 8) or (this[3].toUInt() and 0xFFu)
    }

    private fun ByteArray.toUInt16(): UInt {
        return ((this[0].toUInt() and 0xFFu) shl 8) or (this[1].toUInt() and 0xFFu)
    }
}
