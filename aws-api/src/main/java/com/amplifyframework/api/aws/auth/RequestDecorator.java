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

package com.amplifyframework.api.aws.auth;

import java.io.IOException;

import okhttp3.Request;

/**
 * Interface that defines some basic functionality with regards to decorating
 * HTTP requests that are going to be sent to AppSync
 * Implementations of this class should implement the {@link RequestDecorator#addAuthHeader(com.amazonaws.Request)}
 * method.
 */
public interface RequestDecorator {

    /**
     * Method that takes in an instance of {@link Request}, transforms it as needed
     * and returns a new instance of {@link Request}.
     * @param request the HTTP request before modifications.
     * @return A new instance of the HTTP request after modifications (if any)
     * @throws IOException If an issue occurs during request transformation.
     */
    Request decorate(Request request) throws IOException;
}
