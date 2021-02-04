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

/**
 * Represents server side encryption method types that are supported by AWS S3.
 */
public enum ServerSideEncryption {
    /**
     * AES256 encryption.
     */
    MANAGED_KEYS("AES256"),

    /**
     * aws:kms encryption.
     */
    KMS_KEYS("aws:kms"),

    /**
     * No encryption.
     */
    NONE("");

    private final String name;

    ServerSideEncryption(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the encryption method.
     * @return the name of the encryption method.
     */

    public String getName() {
        return name;
    }
}
