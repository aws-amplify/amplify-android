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

package com.amplifyframework.core.model;

import java.util.Set;

/**
 * Defines the contract for retrieving information about the
 * models generated for DataStore.
 */
public interface ModelStore {
    /**
     * Get a set of the model classes.
     * @return a set of the model classes.
     */
    Set<Class<? extends Model>> list();

    /**
     * Get the version of the models.
     * @return the version string of the models.
     */
    String version();
}
