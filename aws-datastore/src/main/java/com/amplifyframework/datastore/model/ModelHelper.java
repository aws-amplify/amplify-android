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

package com.amplifyframework.datastore.model;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helpers class that contains {@link Model} related utilities.
 */
public final class ModelHelper {
    private static final Logger LOGGER = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

    private ModelHelper() {
        // contains only static helper methods and should not be instantiated
    }

    /**
     * Uses reflection to get a property value on a <code>Model</code> by its <code>ModelField</code>
     * reference. This method prioritizes the property getter and fallback to the field itself
     * if a getter doesn't exist.
     *
     * @param model the model instance
     * @param field the model field
     * @param <M> the model concrete type
     * @return the field value or <code>null</code>
     * @throws DataStoreException in case of a error happens during the dynamic reflection calls
     */
    public static <M extends Model> Object getValue(M model, ModelField field) throws DataStoreException {
        final Class<? extends Model> modelClass = model.getClass();
        final String fieldName = field.getName();
        final String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        try {
            final Method fieldGetter = modelClass.getMethod(getterName);
            return fieldGetter.invoke(model);
        } catch (Exception exception) {
            LOGGER.verbose(String.format(
                    "Could not find %s() on %s. Fallback to direct field access.",
                    getterName, modelClass.getName()
            ));
        }

        // fallback to direct field access
        try {
            final Field fieldReference = modelClass.getDeclaredField(fieldName);
            fieldReference.setAccessible(true);
            return fieldReference.get(model);
        } catch (Exception fallbackException) {
            throw new DataStoreException(
                    "Error when reading the property " + fieldName + " from class " + modelClass.getName(),
                    fallbackException,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }
}
