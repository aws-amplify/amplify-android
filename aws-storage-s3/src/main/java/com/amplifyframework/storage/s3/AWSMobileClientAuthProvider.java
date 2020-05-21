/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;

/**
 * Static utility class internal to the plugin to interface with Auth.
 */
public final class AWSMobileClientAuthProvider implements CognitoAuthProvider {
    private static final String AUTH_DEPENDENCY_PLUGIN_KEY = "awsCognitoAuthPlugin";

    @Override
    public String getIdentityId() throws StorageException {
        try {
            return getMobileClient().getIdentityId();
        } catch (RuntimeException exception) {
            throw new StorageException(
                    "Failed to get user's identity ID",
                    exception,
                    "Please check that you are logged in and that Auth is setup to support identity pools."
            );
        }
    }

    @Override
    public AWSCredentialsProvider getCredentialsProvider() throws StorageException {
        return getMobileClient();
    }

    /**
     * TODO: This is a stop gap measure to quickly and safely integrate the Auth Category before GA. However,
     *  the proper solution is to have categories get AWS credentials from Auth's fetchAuthSession method cast to
     *  an AWS interface. This hasn't yet been made in such a way that categories can depend on it without depending
     *  directly on the AWSCognitoAuthPlugin. After GA, we will make this and replace the direct use of AWSMobileClient.
     * @return The current instance of AWSMobileClient being managed by the Auth Category.
     * @throws StorageException Thrown if the needed Auth plugin has not been added.
     */
    private AWSMobileClient getMobileClient() throws StorageException {
        try {
            return (AWSMobileClient) Amplify.Auth.getPlugin(AUTH_DEPENDENCY_PLUGIN_KEY).getEscapeHatch();
        } catch (IllegalStateException exception) {
            throw new StorageException(
                "AWSS3StoragePlugin depends on AWSCognitoAuthPlugin but it is currently missing",
                exception,
                "Before configuring Amplify, be sure to add AWSCognitoAuthPlugin same as you added AWSS3StoragePlugin."
            );
        }
    }
}
