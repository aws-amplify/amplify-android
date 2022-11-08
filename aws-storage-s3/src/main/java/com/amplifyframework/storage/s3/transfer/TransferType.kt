/**
 * Copyright 2015-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.storage.s3.transfer

/**
 * Represents if a transfer is an upload or download
 */
enum class TransferType {
    /**
     * An Upload operation.
     */
    UPLOAD,
    /**
     * An Download operation.
     */
    DOWNLOAD,
    /**
     * Upload and Download operation.
     */
    ANY;
}
