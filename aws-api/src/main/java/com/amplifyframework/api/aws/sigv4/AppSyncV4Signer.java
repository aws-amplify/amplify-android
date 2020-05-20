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

import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.util.BinaryUtils;

import java.io.InputStream;
import java.net.URI;

/**
 * Signer that signs the request with AppSync-specific
 * service name and region.
 */
public final class AppSyncV4Signer extends AWS4Signer {

    private static final String TAG = AppSyncV4Signer.class.getSimpleName();

    private static final String SERVICE_NAME_SCOPE = "appsync";
    private static final String RESOURCE_PATH = "/graphql";
    private static final String CONNECTION_PATH = "/connect";

    private final boolean isWebSocketConnectionWithIam;

    /**
     * Construct an instance of SigV4 signer for AppSync service using default
     * resource path.
     * @param region the signer region
     */
    public AppSyncV4Signer(String region) {
        this(region, false);
    }

    /**
     * Construct an instance of SigV4 signer for AppSync with an explicit flag
     * whether it's signing for a connection request via AWS IAM or not.
     *
     * A signer that is created with this flag set to true will append "/connect"
     * to its resource path.
     * @param region the signer region
     * @param isWebSocketConnectionWithIam true if signing for web socket connection
     *          request with AWS IAM authentication
     */
    public AppSyncV4Signer(String region, boolean isWebSocketConnectionWithIam) {
        super(true);
        this.isWebSocketConnectionWithIam = isWebSocketConnectionWithIam;
        setRegionName(region);
    }

    @Override
    protected String extractServiceName(URI endpoint) {
        return SERVICE_NAME_SCOPE;
    }

    @Override
    protected String getCanonicalizedResourcePath(String resourcePath) {
        return isWebSocketConnectionWithIam
                ? RESOURCE_PATH + CONNECTION_PATH
                : RESOURCE_PATH;
    }

    @Override
    protected String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        return isWebSocketConnectionWithIam
                ? RESOURCE_PATH + CONNECTION_PATH
                : RESOURCE_PATH;
    }

    @Override
    protected String calculateContentHash(Request<?> request) {
        final InputStream payloadStream = request.getContent();
        payloadStream.mark(-1);
        // We will not reset this as OkHttp does not allow reset of stream.
        return BinaryUtils.toHex(hash(payloadStream));
    }
}
