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

package com.amplifyframework.auth.cognito

import aws.sdk.kotlin.runtime.http.operation.customUserAgentMetadata
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.endpoints.CognitoIdentityProviderEndpointProvider
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration

interface AWSCognitoAuthService {
    val cognitoIdentityProviderClient: CognitoIdentityProviderClient?
    val cognitoIdentityClient: CognitoIdentityClient?
    val customUserAgentPairs: MutableMap<String, String>

    companion object {
        internal fun fromConfiguration(configuration: AuthConfiguration): AWSCognitoAuthService {
            val customPairs: MutableMap<String, String> = mutableMapOf()
            val cognitoIdentityProviderClient = configuration.userPool?.let { it ->
                CognitoIdentityProviderClient {
                    this.region = it.region
                    this.endpointProvider = it.endpoint?.let { endpoint ->
                        CognitoIdentityProviderEndpointProvider { Endpoint(endpoint) }
                    }
                    this.interceptors += object : HttpInterceptor {
                        override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
                            customPairs.forEach { (key, value) ->
                                context.executionContext.customUserAgentMetadata.add(key, value)
                            }
                            return super.modifyBeforeSerialization(context)
                        }
                    }
                }
            }

            val cognitoIdentityClient = configuration.identityPool?.let { it ->
                CognitoIdentityClient {
                    this.region = it.region
                    this.interceptors += object : HttpInterceptor {
                        override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
                            customPairs.forEach { (key, value) ->
                                context.executionContext.customUserAgentMetadata.add(key, value)
                            }
                            return super.modifyBeforeSerialization(context)
                        }
                    }
                }
            }

            return object : AWSCognitoAuthService {
                override val cognitoIdentityProviderClient = cognitoIdentityProviderClient
                override val cognitoIdentityClient = cognitoIdentityClient
                override val customUserAgentPairs = customPairs
            }
        }
    }
}
