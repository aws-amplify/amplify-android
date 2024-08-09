/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.datastore.extensions

import com.amplifyframework.core.model.Model

/**
 * This method returns the primary key that was used on the ModelMetadata table for the given model.
 * The returned value should only be used to construct the lookup sqlite key, and is not a value used by AppSync
 * @return the primary key that was used on the ModelMetadata table for the given model
 */
internal fun Model.getMetadataSQLitePrimaryKey(): String {
    return "$modelName|$primaryKeyString"
}
