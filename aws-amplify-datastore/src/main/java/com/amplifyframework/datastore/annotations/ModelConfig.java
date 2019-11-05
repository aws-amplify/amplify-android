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

package com.amplifyframework.datastore.annotations;

/**
 * {@link ModelConfig} annotates any {@link com.amplifyframework.datastore.model.Model}
 * with the configuration information that is applicable to a
 * {@link com.amplifyframework.datastore.model.Model}.
 */
public @interface ModelConfig {
    /**
     * Specifies the name of the Model in the target.
     * @return the name of the Model in the target.
     *         For example: the name of the Model in
     *         the GraphQL schema.
     */
    String targetName() default "";
}
