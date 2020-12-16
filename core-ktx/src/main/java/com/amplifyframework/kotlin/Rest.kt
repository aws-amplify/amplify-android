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

package com.amplifyframework.kotlin

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.api.rest.RestResponse

interface Rest {
    /**
     * Issue a GET request against an API.
     * @param request Request options
     * @param apiName One of the named APIs in your configuration file;
     *                if not specified, uses the first REST API found
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun get(request: RestOptions, apiName: String? = null): RestResponse

    /**
     * Issue a PUT request against an API.
     * @param request Request options
     * @param apiName One of the named APIs in your configuration file;
     *                if not specified, uses the first REST API found
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun put(request: RestOptions, apiName: String? = null): RestResponse

    /**
     * Issue a POST request against an API.
     * @param request Request options
     * @param apiName One of the named APIs in your configuration file;
     *                if not specified, uses the first REST API found
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun post(request: RestOptions, apiName: String? = null): RestResponse

    /**
     * Issue a DELETE request against an API.
     * @param request Request options
     * @param apiName One of the named APIs in your configuration file;
     *                if not specified, uses the first REST API found
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun delete(request: RestOptions, apiName: String? = null): RestResponse

    /**
     * Issue a HEAD request against an API.
     * @param request Request options
     * @param apiName One of the named APIs in your configuration file;
     *                if not specified, uses the first REST API found
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun head(request: RestOptions, apiName: String? = null): RestResponse

    /**
     * Issue a PATCH request against an API.
     * @param request Request options
     * @param apiName One of the named APIs in your configuration file;
     *                if not specified, uses the first REST API found
     * @return Response
     */
    @Throws(ApiException::class)
    suspend fun patch(request: RestOptions, apiName: String? = null): RestResponse
}
