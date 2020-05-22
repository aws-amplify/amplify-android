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

/**
 * Internal interface for providing AWS specific Auth information.
 */
public interface CognitoAuthProvider {
    /**
     * Get the identity ID of the currently logged in user if they are registered in identity pools.
     * @return the identity ID of the currently logged in user.
     * @throws StorageException  If the proper Auth plugin isn't added or identity id is unavailable
     */
    String getIdentityId() throws StorageException;

    /**
     * Get an object which implements the AWSCredentialsProvider interface to get the credentials needed by storage.
     * @return an object which implements the AWSCredentialsProvider interface to get the credentials needed by storage
     * @throws StorageException  If the proper Auth plugin isn't added
     */
    AWSCredentialsProvider getCredentialsProvider() throws StorageException;
}
