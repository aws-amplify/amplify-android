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

package com.amplifyframework.datastore;

import com.amplifyframework.core.model.ModelField;

/**
 * Establishes how `Model` fields should be converted to and from different targets (e.g. SQL and GraphQL).
 *
 * @param <S> The source type (e.g. SQLite's <code>Cursor</code>)
 * @param <T> The target type, usually the <code>Model</code>
 */
public interface ModelFieldTypeConverter<S, T> {

    /**
     * Converts a specific <code>ModelField</code> from the <code>S</code> to a value compatible
     * with the <code>T</code> type.
     *
     * @param source the source object instance (e.g. <code>Cursor</code>)
     * @param field the field metadata
     * @return the value converted to the right type given the field metadata
     * @throws DataStoreException if something unexpected happens during type conversion
     */
    Object convertValueFromSource(S source, ModelField field) throws DataStoreException;

    /**
     * Converts a specific <code>ModelField</code> from the <code>T</code> to a value compatible
     * with the <code>S</code> type.
     *
     * @param target the target object instance (e.g. <code>Model</code>)
     * @param field the field metadata
     * @return the value converted to the right type given the field metadata
     * @throws DataStoreException if something unexpected happens during type conversion
     */
    Object convertValueFromTarget(T target, ModelField field) throws DataStoreException;
}
