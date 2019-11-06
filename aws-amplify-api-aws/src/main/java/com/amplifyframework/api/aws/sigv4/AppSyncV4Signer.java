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
@SuppressWarnings("UnnecessaryLocalVariable") // This is legacy code.
public final class AppSyncV4Signer extends AWS4Signer {

    private static final String TAG = AppSyncV4Signer.class.getSimpleName();

    private static final String SERVICE_NAME_SCOPE = "appsync";

    private static final String RESOURCE_PATH = "/graphql";

    private ResourcePath resourcePath;

    /**
     * url in the canonical request for connecting to gogi via AWS_IAM requires "/connect"
     * appended to it.
     *
     */
    public enum ResourcePath {
        IAM_CONNECTION_RESOURCE_PATH,
        DEFAULT_RESOURCE_PATH;
    }

    public AppSyncV4Signer(String region) {
        super(true);
        setRegionName(region);
    }

    public AppSyncV4Signer(String region, ResourcePath resourcePath) {
        super(true);
        this.resourcePath = resourcePath;
        setRegionName(region);
    }

    @Override
    protected String extractServiceName(URI endpoint) {
        return SERVICE_NAME_SCOPE;
    }

    @Override
    protected String getCanonicalizedResourcePath(String resourcePath) {
        return (this.resourcePath != null && this.resourcePath.equals(ResourcePath.IAM_CONNECTION_RESOURCE_PATH)) ?
                RESOURCE_PATH + "/connect" : RESOURCE_PATH;
    }

    @Override
    protected String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        return (this.resourcePath != null && this.resourcePath.equals(ResourcePath.IAM_CONNECTION_RESOURCE_PATH)) ?
                RESOURCE_PATH + "/connect" : RESOURCE_PATH;
    }

    @Override
    protected String calculateContentHash(Request<?> request) {
        final InputStream payloadStream = request.getContent();
        payloadStream.mark(-1);
        final String contentSha256 = BinaryUtils.toHex(hash(payloadStream));
        // We will not reset this as ok http does not allow reset of stream.
        return contentSha256;
    }
}
