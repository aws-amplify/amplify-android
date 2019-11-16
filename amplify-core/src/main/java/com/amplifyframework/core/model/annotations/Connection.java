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

package com.amplifyframework.core.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate that a field of a class which extends
 * {@link com.amplifyframework.core.model.Model} describes a connection with
 * another class another class that extends
 * {@link com.amplifyframework.core.model.Model}.
 *
 * The {@link RetentionPolicy#RUNTIME} annotation is added to
 * retain {@link Connection} at runtime for the reflection capabilities to work
 * in order to check if this annotation is present for a field of a Model.
 *
 * {@link ElementType#FIELD} annotation is added to indicate
 * {@link Connection} annotation can be used only on Fields (Data members of a class).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Connection {
    /**
     * Unique identifier for the connection.
     * @return name of the connection
     */
    String name();

    /**
     * Gets the name of the foreign key.
     * @return name of the foreign key
     */
    String keyField();

    /**
     * Gets the sort key equivalent in DynamoDB.
     * @return name of the sort key
     */
    String sortField();

    /**
     * Gets the key name used for indexing in DynamoDB.
     * @return name of the key
     */
    String keyName();

    /**
     * Gets the pagination limit.
     * @return pagination limit
     */
    int limit();

    /**
     * Sorted collection of keyField, sortField, and
     * additional fields.
     * @return array of fields
     */
    String[] fields();

    /**
     * Gets the relationship type that model has with
     * annotated field.
     * @return relationship type with field
     */
    String relationship();
}
