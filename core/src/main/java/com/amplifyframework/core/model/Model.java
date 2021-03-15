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

import androidx.annotation.NonNull;

/**
 * All models should conform to the Model
 * interface.
 */
public interface Model {
    /**
     * Return the ID that is the primary key
     * of a Model.
     *
     * @return the ID that is the primary key of a Model.
     */
    @NonNull
    String getId();

    /**
     * Returns the name of this model as a String.
     * @return the name of this model as a String.
     */
    @NonNull
    default String getModelName() {
        return getClass().getSimpleName();
    }
}
