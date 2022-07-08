/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.AmplifyException;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility for converting a Model to/from a Map&lt;String, Object&gt;.
 */
public final class ModelConverter {

    private ModelConverter() {}

    /**
     * Convert a Model to a Map&lt;String, Object&gt;.
     * @param instance a Model instance.
     * @param schema a Model schema for the instance.
     * @param <T> type of the Model instance.
     * @return a Map&lt;String, Object&gt; representation of the provided Model instance.
     * @throws AmplifyException if schema doesn't match instance
     */
    public static <T extends Model> Map<String, Object> toMap(T instance, ModelSchema schema) throws AmplifyException {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        final Map<String, Object> result = new HashMap<>();
        for (ModelField modelField : schema.getFields().values()) {
            String fieldName = modelField.getName();
            String targetType = modelField.getTargetType();
            final ModelAssociation association = schema.getAssociations().get(fieldName);
            if (association == null) {
                if (instance instanceof SerializedModel
                        && !((SerializedModel) instance).getSerializedData().containsKey(modelField.getName())) {
                    // Skip fields that are not set, so that they are not set to null in the request.
                    continue;
                }
                result.put(fieldName, extractFieldValue(modelField.getName(), instance, schema));
            } else if (association.isOwner()) {
                ModelSchema nestedSchema = schemaRegistry.getModelSchemaForModelClass(targetType);
                Map<String, Object> associateId = extractAssociateId(modelField, instance, schema);
                if (associateId == null) {
                    // Skip fields that are not set, so that they are not set to null in the request.
                    continue;
                }
                result.put(fieldName, SerializedModel.builder()
                    .modelSchema(nestedSchema)
                    .serializedData(associateId)
                    .build());
            }
            // Ignore if field is associated, but is not a "belongsTo" relationship
        }
        return result;
    }

    private static Map<String, Object> extractAssociateId(ModelField modelField, Model instance,
                                                                    ModelSchema schema)
            throws AmplifyException {
        final Object fieldValue = extractFieldValue(modelField.getName(), instance, schema);
        if (modelField.isModel() && fieldValue instanceof Model) {
            Model associatedModel = (Model) fieldValue;
            ModelSchema childSchema =
                    SchemaRegistry.instance().getModelSchemaForModelClass(associatedModel.getModelName());
            /* Loop through primary key fields and get their respective values from the key map.
              Deserialize the key value to the model with primary key values populated.
             */

            HashMap<String, Object> hashMap = new HashMap<>();
            if (childSchema.getPrimaryIndexFields().size() > 1 && (associatedModel.resolveIdentifier()
                    instanceof ModelIdentifier)) {
                ModelIdentifier<? extends Model> primaryKey =
                        (ModelIdentifier<? extends Model>) associatedModel.resolveIdentifier();
                Iterator<String> pkFieldIterator = childSchema.getPrimaryIndexFields().listIterator();
                hashMap.put(pkFieldIterator.next(), primaryKey.key());
                Iterator<? extends Serializable> sortKeyIterator = primaryKey.sortedKeys().listIterator();

                while (pkFieldIterator.hasNext()) {
                    hashMap.put(pkFieldIterator.next(), sortKeyIterator.next());
                }
                return hashMap;
            } else if (childSchema.getPrimaryIndexFields().size() > 1 &&
                    (associatedModel instanceof SerializedModel)) {
                for (String pkField : childSchema.getPrimaryIndexFields()) {
                    hashMap.put(pkField, ((SerializedModel) associatedModel).getSerializedData().get(pkField));
                }
                return hashMap;
            } else {
                // Create dummy model instance using just the ID and model type
                return Collections.singletonMap(childSchema.getPrimaryIndexFields().get(0),
                        associatedModel.getPrimaryKeyString());
            }

        } else if (modelField.isModel() && fieldValue instanceof Map) {
            return Collections.singletonMap("id", ((Map<?, ?>) fieldValue).get("id"));
        } else if (modelField.isModel() && fieldValue == null) {
            return null;
        } else {
            throw new IllegalStateException("Associated data is not a Model or Map.");
        }
    }

    private static Object extractFieldValue(String fieldName, Model instance, ModelSchema schema)
            throws AmplifyException {
        if (instance instanceof SerializedModel) {
            SerializedModel serializedModel = (SerializedModel) instance;
            Map<String, Object> serializedData = serializedModel.getSerializedData();
            return serializedData.get(fieldName);
        }
        try {
            Field privateField = instance.getClass().getDeclaredField(fieldName);
            privateField.setAccessible(true);
            return privateField.get(instance);
        } catch (Exception exception) {
            throw new AmplifyException(
                    "An invalid field was provided. " + fieldName + " is not present in " + schema.getName(),
                    exception,
                    "Check if this model schema is a correct representation of the fields in the provided Object");
        }
    }
}
