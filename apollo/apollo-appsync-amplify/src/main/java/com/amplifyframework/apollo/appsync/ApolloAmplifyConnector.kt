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

package com.amplifyframework.apollo.appsync

import android.content.Context
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.apollo.appsync.authorizers.ApiKeyAuthorizer
import com.amplifyframework.apollo.appsync.authorizers.AuthTokenAuthorizer
import com.amplifyframework.apollo.appsync.authorizers.IamAuthorizer
import com.amplifyframework.apollo.appsync.util.AccessTokenProvider
import com.amplifyframework.apollo.appsync.util.ApolloRequestSigner
import com.amplifyframework.apollo.appsync.util.safeCallInGlobalScope
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.apollographql.apollo.api.http.HttpRequest
import java.util.function.Consumer

/**
 * Connect Apollo to AppSync by delegating to Amplify for tokens and signing.
 */
class ApolloAmplifyConnector internal constructor(
    data: AmplifyOutputsData.Data,
    private val requestSigner: ApolloRequestSigner = ApolloRequestSigner(),
    private val accessTokenProvider: AccessTokenProvider = AccessTokenProvider()
) {
    /**
     * Instantiate a connector with the configuration for an Amplify Gen2 App
     * @param context A [Context] instance to read resources from
     * @param outputs The [AmplifyOutputs] to read
     */
    @OptIn(InternalAmplifyApi::class)
    constructor(
        context: Context,
        outputs: AmplifyOutputs
    ) : this(AmplifyOutputsData.deserialize(context, outputs).data ?: error("No data section in AmplifyOutputs"))

    /**
     * The [AppSyncEndpoint] instance pointing to the endpoint specified in the Amplify Outputs
     */
    val endpoint: AppSyncEndpoint = AppSyncEndpoint(data.url)

    /**
     * The AWS Region specified in the Amplify Outputs
     */
    val region: String = data.awsRegion

    /**
     * The API Key specified in the Amplify Outputs, if any
     */
    val apiKey: String? = data.apiKey

    /**
     * Create an [AppSyncAuthorizer] instance that uses the API Key specified in your Amplify Outputs.
     * @return An [AppSyncAuthorizer] to use when setting up Apollo
     * @throws IllegalStateException if your configuration does not contain an API Key
     */
    fun apiKeyAuthorizer() = ApiKeyAuthorizer(apiKey ?: error("No API key configured for data category"))

    /**
     * Create an [AppSyncAuthorizer] instance that authorizes users based on the Cognito User pool set in your
     * configuration. To use this authorizer you must configure Amplify while adding the aws-auth-cognito plugin.
     * @return An [AppSyncAuthorizer] to use when setting up Apollo
     */
    fun cognitoUserPoolAuthorizer() = AuthTokenAuthorizer(accessTokenProvider::fetchLatestCognitoAuthToken)

    /**
     * Create an [AppSyncAuthorizer] that signs requests for IAM authorization. To use this authorizer you must configure
     * Amplify while adding the aws-auth-cognito plugin.
     * @return An [AppSyncAuthorizer] to use when setting up Apollo
     */
    fun iamAuthorizer() = IamAuthorizer { requestSigner.signAppSyncRequest(it, region) }

    companion object {

        private val requestSigner = ApolloRequestSigner()
        private val accessTokenProvider = AccessTokenProvider()

        /**
         * Signs an [HttpRequest] for the given region
         */
        @JvmSynthetic
        suspend fun signAppSyncRequest(request: HttpRequest, region: String): Map<String, String> =
            requestSigner.signAppSyncRequest(request, region)

        /**
         * Signs an [HttpRequest] for the given region using a callback. This version of the function is intended for
         * Java usage.
         */
        @JvmStatic
        fun signAppSyncRequest(
            request: HttpRequest,
            region: String,
            onSuccess: Consumer<Map<String, String>>,
            onError: Consumer<Throwable>
        ) = safeCallInGlobalScope(onSuccess, onError) { signAppSyncRequest(request, region) }

        /**
         * Returns the latest Cognito auth token from Amplify
         */
        @JvmSynthetic
        suspend fun fetchLatestCognitoAuthToken() = accessTokenProvider.fetchLatestCognitoAuthToken()

        /**
         * Returns the latest Cognito auth token from Amplify using a callback. This version of the function
         * is intended for Java usage.
         */
        @JvmStatic
        fun fetchLatestCognitoAuthToken(onSuccess: Consumer<String>, onError: Consumer<Throwable>) =
            safeCallInGlobalScope(onSuccess, onError, accessTokenProvider::fetchLatestCognitoAuthToken)
    }
}
