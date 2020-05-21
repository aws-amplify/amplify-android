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

import com.amplifyframework.storage.StorageException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;

/**
 * Allows integration tests to integrate with Auth functionality.
 * TODO: Update Storage integration tests to go through the Auth Category not AWSMobileClient directly.
 */
public final class TestCognitoAuthProvider implements CognitoAuthProvider {
    @Override
    public String getIdentityId() throws StorageException {
        try {
            return AWSMobileClient.getInstance().getIdentityId();
        } catch (RuntimeException exception) {
            throw new StorageException(
                    "Failed to get user's identity ID",
                    exception,
                    "Please check that you are logged in and that Auth is setup to support identity pools."
            );
        }
    }

    @Override
    public AWSCredentialsProvider getCredentialsProvider() {
        return AWSMobileClient.getInstance();
    }
}
