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

package com.amplifyframework.storage;

import android.support.annotation.NonNull;

import com.amplifyframework.storage.exception.*;
import com.amplifyframework.storage.operation.*;
import com.amplifyframework.storage.option.*;

import java.io.File;

public interface StorageCategoryClientBehavior {
    StorageGetOperation get(@NonNull String key, StorageGetOption option) throws StorageGetException;

    StoragePutOperation put(@NonNull String key, @NonNull File file, StoragePutOption option) throws StoragePutException;

    StoragePutOperation put(@NonNull String key, @NonNull String path, StoragePutOption option) throws StoragePutException;

    StorageListOperation list(StorageListOption option) throws StorageListException;

    StorageRemoveOperation remove(@NonNull String key, StorageRemoveOption option) throws StorageRemoveException;
}
