/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.annotations.InternalAmplifyApi;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.predicate.BeginsWithQueryOperator;
import com.amplifyframework.core.model.query.predicate.BetweenQueryOperator;
import com.amplifyframework.core.model.query.predicate.ContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

/**
 * Although this has public access, it is intended for internal use and should not be used directly by host
 * applications. The behavior of this may change without warning.
 */
@InternalAmplifyApi
@SuppressWarnings("HideUtilityClassConstructor")
public class GraphQLRequestHelper {

    private static String appSyncOpType(QueryOperator.Type type) throws AmplifyException {
        switch (type) {
            case NOT_EQUAL:
                return "ne";
            case EQUAL:
                return "eq";
            case LESS_OR_EQUAL:
                return "le";
            case LESS_THAN:
                return "lt";
            case GREATER_OR_EQUAL:
                return "ge";
            case GREATER_THAN:
                return "gt";
            case CONTAINS:
                return "contains";
            case BETWEEN:
                return "between";
            case BEGINS_WITH:
                return "beginsWith";
            default:
                throw new AmplifyException(
                        "Tried to parse an unsupported QueryOperator type",
                        "Check if a new QueryOperator.Type enum has been created which is not supported " +
                                "in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    private static Object appSyncOpValue(QueryOperator<?> qOp) throws AmplifyException {
        switch (qOp.type()) {
            case NOT_EQUAL:
                return ((NotEqualQueryOperator) qOp).value();
            case EQUAL:
                return ((EqualQueryOperator) qOp).value();
            case LESS_OR_EQUAL:
                return ((LessOrEqualQueryOperator<?>) qOp).value();
            case LESS_THAN:
                return ((LessThanQueryOperator<?>) qOp).value();
            case GREATER_OR_EQUAL:
                return ((GreaterOrEqualQueryOperator<?>) qOp).value();
            case GREATER_THAN:
                return ((GreaterThanQueryOperator<?>) qOp).value();
            case CONTAINS:
                return ((ContainsQueryOperator) qOp).value();
            case NOT_CONTAINS:
                return ((NotContainsQueryOperator) qOp).value();
            case BETWEEN:
                BetweenQueryOperator<?> betweenOp = (BetweenQueryOperator<?>) qOp;
                return Arrays.asList(betweenOp.start(), betweenOp.end());
            case BEGINS_WITH:
                return ((BeginsWithQueryOperator) qOp).value();
            default:
                throw new AmplifyException(
                        "Tried to parse an unsupported QueryOperator type",
                        "Check if a new QueryOperator.Type enum has been created which is not supported " +
                                "in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    @InternalAmplifyApi
    @SuppressWarnings("MissingJavadocMethod")
    public static Map<String, Object> parsePredicate(QueryPredicate queryPredicate) throws AmplifyException {
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation<?> qpo = (QueryPredicateOperation<?>) queryPredicate;
            QueryOperator<?> op = qpo.operator();
            return Collections.singletonMap(
                    qpo.field(),
                    Collections.singletonMap(appSyncOpType(op.type()), appSyncOpValue(op))
            );
        } else if (queryPredicate instanceof QueryPredicateGroup) {
            QueryPredicateGroup qpg = (QueryPredicateGroup) queryPredicate;

            if (QueryPredicateGroup.Type.NOT.equals(qpg.type())) {
                try {
                    return Collections.singletonMap("not", parsePredicate(qpg.predicates().get(0)));
                } catch (IndexOutOfBoundsException exception) {
                    throw new AmplifyException(
                            "Predicate group of type NOT must include a value to negate.",
                            exception,
                            "Check if you created a NOT condition in your Predicate with no included value."
                    );
                }
            } else {
                List<Map<String, Object>> predicates = new ArrayList<>();

                for (QueryPredicate predicate : qpg.predicates()) {
                    predicates.add(parsePredicate(predicate));
                }

                return Collections.singletonMap(qpg.type().toString().toLowerCase(Locale.getDefault()), predicates);
            }
        } else {
            throw new AmplifyException(
                    "Tried to parse an unsupported QueryPredicate",
                    "Try changing to one of the supported values: QueryPredicateOperation, QueryPredicateGroup."
            );
        }
    }

    @InternalAmplifyApi
    @SuppressWarnings("MissingJavadocMethod")
    public static Map<String, Object> getDeleteMutationInputMap(
            @NonNull ModelSchema schema, @NonNull Model instance) throws AmplifyException {
        final Map<String, Object> input = new HashMap<>();
        for (String fieldName : schema.getPrimaryIndexFields()) {
            input.put(fieldName, extractFieldValue(fieldName, instance, schema));
        }
        return input;
    }

    @InternalAmplifyApi
    @SuppressWarnings("MissingJavadocMethod")
    public static Map<String, Object> getMapOfFieldNameAndValues(
            @NonNull ModelSchema schema, @NonNull Model instance, MutationType type) throws AmplifyException {
        boolean isSerializedModel = instance instanceof SerializedModel;
        boolean hasMatchingModelName = instance.getClass().getSimpleName().equals(schema.getName());
        if (!(hasMatchingModelName || isSerializedModel)) {
            throw new AmplifyException(
                    "The object provided is not an instance of " + schema.getName() + ".",
                    "Please provide an instance of " + schema.getName() + " that matches the schema type."
            );
        }

        Map<String, Object> result = new HashMap<>(extractFieldLevelData(schema, instance, type));

        /*
         * If the owner field exists on the model, and the value is null, it should be omitted when performing a
         * mutation because the AppSync server will automatically populate it using the authentication token provided
         * in the request header.  The logic below filters out the owner field if null for this scenario.
         */
        for (AuthRule authRule : schema.getAuthRules()) {
            if (AuthStrategy.OWNER.equals(authRule.getAuthStrategy())) {
                String ownerField = authRule.getOwnerFieldOrDefault();
                if (result.containsKey(ownerField) && result.get(ownerField) == null) {
                    result.remove(ownerField);
                }
            }
        }

        return result;
    }

    private static Map<String, Object> extractFieldLevelData(
            ModelSchema schema, Model instance, MutationType type) throws AmplifyException {
        final Map<String, Object> result = new HashMap<>();
        for (ModelField modelField : schema.getFields().values()) {
            if (modelField.isReadOnly()) {
                // Skip read only fields, since they should not be included on the input object.
                continue;
            }
            String fieldName = modelField.getName();
            final ModelAssociation association = schema.getAssociations().get(fieldName);
            if (instance instanceof SerializedModel
                    && !((SerializedModel) instance).getSerializedData().containsKey(fieldName)) {
                // Skip fields that are not set, so that they are not set to null in the request.
                continue;
            }

            Object fieldValue = extractFieldValue(modelField.getName(), instance, schema);

            if (association == null) {
                result.put(fieldName, fieldValue);
            } else if (association.isOwner()) {
                if (fieldValue == null && MutationType.CREATE.equals(type)) {
                    // Do not set null values on associations for create mutations.
                } else if (schema.getVersion() >= 1 && association.getTargetNames() != null
                        && association.getTargetNames().length > 0) {
                    // When target name length is more than 0 there are two scenarios, one is when
                    // there is custom primary key and other is when we have composite primary key.
                    insertForeignKeyValues(result, modelField, fieldValue, association);
                } else {
                    String targetName = association.getTargetName();
                    result.put(targetName, extractAssociateId(modelField, fieldValue));
                }
            }
            // Ignore if field is associated, but is not a "belongsTo" relationship
        }
        return result;
    }

    private static void insertForeignKeyValues(
            Map<String, Object> result,
            ModelField modelField,
            Object fieldValue,
            ModelAssociation association) {
        if (modelField.isModel() && fieldValue == null) {
            // When there is no model field value, set null for removal of values or deassociation.
            for (String key : association.getTargetNames()) {
                result.put(key, null);
            }
        } else if (modelField.isModel() && fieldValue instanceof Model) {
            if (((Model) fieldValue).resolveIdentifier() instanceof ModelIdentifier<?>) {
                final ModelIdentifier<?> primaryKey = (ModelIdentifier<?>) ((Model) fieldValue).resolveIdentifier();
                ListIterator<String> targetNames = Arrays.asList(association.getTargetNames()).listIterator();
                Iterator<? extends Serializable> sortedKeys = primaryKey.sortedKeys().listIterator();

                result.put(targetNames.next(), primaryKey.key());

                while (targetNames.hasNext()) {
                    result.put(targetNames.next(), sortedKeys.next());
                }
            } else if ((fieldValue instanceof SerializedModel)) {
                SerializedModel serializedModel = ((SerializedModel) fieldValue);
                ModelSchema serializedSchema = serializedModel.getModelSchema();
                if (serializedSchema != null &&
                        serializedSchema.getPrimaryIndexFields().size() > 1) {

                    ListIterator<String> primaryKeyFieldsIterator = serializedSchema.getPrimaryIndexFields()
                            .listIterator();
                    for (String targetName : association.getTargetNames()) {
                        result.put(targetName, serializedModel.getSerializedData()
                                .get(primaryKeyFieldsIterator.next()));
                    }
                } else {
                    result.put(association.getTargetNames()[0], ((Model) fieldValue).resolveIdentifier().toString());
                }
            } else {
                result.put(association.getTargetNames()[0], ((Model) fieldValue).resolveIdentifier().toString());
            }
        }
    }

    private static Object extractAssociateId(ModelField modelField, Object fieldValue) {
        if (modelField.isModel() && fieldValue instanceof Model) {
            return ((Model) fieldValue).resolveIdentifier();
        } else if (modelField.isModel() && fieldValue instanceof Map) {
            return ((Map<?, ?>) fieldValue).get("id");
        } else if (modelField.isModel() && fieldValue == null) {
            // When there is no model field value, set null for removal of values or deassociation.
            return null;
        } else {
            throw new IllegalStateException("Associated data is not Model or Map.");
        }
    }

    private static Object extractFieldValue(String fieldName, Model instance, ModelSchema schema)
            throws AmplifyException {
        if (instance instanceof SerializedModel) {
            SerializedModel serializedModel = (SerializedModel) instance;
            Map<String, Object> serializedData = serializedModel.getSerializedData();
            ModelField field = schema.getFields().get(fieldName);
            Object fieldValue = serializedData.get(fieldName);
            if (fieldValue != null && field != null && field.isCustomType()) {
                return extractCustomTypeFieldValue(fieldName, serializedData.get(fieldName));
            }
            return fieldValue;
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

    private static Object extractCustomTypeFieldValue(String fieldName, Object customTypeData) throws AmplifyException {
        // Flutter use case:
        // If a field is a CustomType, it's value is either a SerializedCustomType
        // or a List of SerializedCustomType
        if (customTypeData instanceof SerializedCustomType) {
            return ((SerializedCustomType) customTypeData).getFlatSerializedData();
        }

        if (customTypeData instanceof List) {
            ArrayList<Object> result = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Object> customTypeList = (List<Object>) customTypeData;
            for (Object item : customTypeList) {
                if (item instanceof SerializedCustomType) {
                    result.add(((SerializedCustomType) item).getFlatSerializedData());
                } else {
                    result.add(item);
                }
            }
            return result;
        }

        throw new AmplifyException(
                "An invalid CustomType field was provided. " + fieldName + " must be an instance of " +
                        "SerializedCustomType or a List of instances of SerializedCustomType",
                "Check if this model schema is a correct representation of the fields in the provided Object");
    }
}
