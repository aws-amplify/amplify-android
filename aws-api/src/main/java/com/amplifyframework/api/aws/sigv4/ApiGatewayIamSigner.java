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

package com.amplifyframework.api.aws.sigv4;

import com.amazonaws.auth.AWS4Signer;

import java.net.URI;

/**
 * Handles signing of APIGateway request using sigv4 signing.
 */
final class ApiGatewayIamSigner extends AWS4Signer {

    private static final String SERVICE_NAME_SCOPE = "execute-api";

    /**
     * Constructs a {@link ApiGatewayIamSigner}.
     * @param region Region where the api gateway is defined.
     */
    ApiGatewayIamSigner(String region) {
        super(true);
        setRegionName(region);
    }

    /**
     * Use the correct service name.
     * @param endpoint Region where the api gateway is defined.
     * @return The service name to be used for signing.
     */
    @Override
    protected String extractServiceName(URI endpoint) {
        return SERVICE_NAME_SCOPE;
    }

    /**
     * Overrides the base method getCanonicalizedResourcePath.
     *
     * API Gateway signing does not work if there is a trailing /.
     * @param resourcePath The resourcepath of the request.
     * @param urlEncode Boolean to do url encoding.
     * @return Canonicalized resource path.
     */
    @Override
    protected String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        String canonicalizedPath = super.getCanonicalizedResourcePath(resourcePath, urlEncode);
        if (canonicalizedPath.endsWith("/")) {
            canonicalizedPath = canonicalizedPath.substring(0, canonicalizedPath.length() - 1);
        }
        return canonicalizedPath;
    }
}
