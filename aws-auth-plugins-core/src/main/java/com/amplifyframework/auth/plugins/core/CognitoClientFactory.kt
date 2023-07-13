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

package com.amplifyframework.auth.plugins.core

import aws.sdk.kotlin.runtime.http.operation.customUserAgentMetadata
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import com.amplifyframework.auth.AWSCognitoAuthMetadataType
import com.amplifyframework.auth.plugins.core.data.AWSCognitoIdentityPoolConfiguration
import com.amplifyframework.plugins.core.BuildConfig

internal object CognitoClientFactory {
    fun createIdentityClient(
        identityPool: AWSCognitoIdentityPoolConfiguration,
        pluginKey: String,
        pluginVersion: String,
    ) = CognitoIdentityClient {
        this.region = identityPool.region
        this.interceptors += object : HttpInterceptor {
            override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
                context.executionContext.customUserAgentMetadata.add(
                    AWSCognitoAuthMetadataType.AuthPluginsCore.key,
                    BuildConfig.VERSION_NAME
                )
                context.executionContext.customUserAgentMetadata.add(pluginKey, pluginVersion)
                return super.modifyBeforeSerialization(context)
            }
        }
    }
}
