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

package com.amplifyframework.geo.location.service

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.services.geo.AmazonLocationClient
import com.amazonaws.services.geo.model.GetMapStyleDescriptorRequest
import com.amplifyframework.util.UserAgent

import kotlinx.coroutines.*
import java.nio.ByteBuffer

/**
 * Implements the backend provider for the location plugin using
 * AWS Mobile SDK's [AmazonLocationClient].
 * @param credentialsProvider AWS credentials provider for authorizing API calls
 * @param region AWS region for the Amazon Location Service
 */
class AmazonLocationService(
    credentialsProvider: AWSCredentialsProvider,
    region: String
) : GeoService<AmazonLocationClient> {
    override val provider: AmazonLocationClient

    init {
        val configuration = ClientConfiguration()
        configuration.userAgent = UserAgent.string()
        provider = AmazonLocationClient(credentialsProvider, configuration)
        provider.setRegion(Region.getRegion(region))
    }

    override suspend fun getStyleJson(mapName: String): String {
        return withContext(Dispatchers.IO) {
            val request = GetMapStyleDescriptorRequest()
                .withMapName(mapName)
            val response = provider.getMapStyleDescriptor(request)
            readFromBuffer(response.blob)
        }
    }

    private fun readFromBuffer(buffer: ByteBuffer): String {
        return if (buffer.hasArray()) {
            val startIndex = buffer.arrayOffset() + buffer.position()
            val endIndex = startIndex + buffer.remaining()
            buffer.array().decodeToString(startIndex, endIndex)
        } else {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes.decodeToString()
        }
    }
}