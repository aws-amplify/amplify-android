/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api;

/**
 * A response factory is able to generate strongly-typed response
 * objects from a string that was returned from an API.
 */
public interface ResponseFactory {
    /**
     * Builds a response object from a string response from a API.
     * @param apiResponseJson
     *        Response from the endpoint, containing a string response
     * @param classToCast
     *        The class type to which the JSON string should be
     *        interpreted
     * @param <T> The type of the data field in the response object
     * @return An instance of the casting class which models the data
     *         provided in the response JSON string
     */
    <T> Response<T> buildResponse(String apiResponseJson, Class<T> classToCast);
}

