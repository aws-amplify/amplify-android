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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.BeginsWithQueryOperator;
import com.amplifyframework.core.model.query.predicate.BetweenQueryOperator;
import com.amplifyframework.core.model.query.predicate.ContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.FieldFinder;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A factory to generate requests against an AppSync endpoint.
 *
 * This is an implementation detail of the {@link AppSyncClient}.
 *
 * AppSync requests are raw GraphQL document strings, which contain AppSync-specific details,
 * such as AppSync mutation names (create, update, delete, and associated subscription names),
 * and AppSync-specific field names (`_version`, `_deleted`, etc.)
 */
final class AppSyncRequestFactory {
    private static final int WALK_DEPTH = 1;
    private static final List<String> ITEM_SYNC_KEYS = Arrays.asList(
        "_version",
        "_deleted",
        "_lastChangedAt"
    );

    private AppSyncRequestFactory() {}

    /**
     * Builds the query document for base and delta sync.
     * If you provide lastSyncTime, it builds a delta sync, where the delta is computed
     * against the provided time. Otherwise, if you provide a null lastSyncTime, a
     * request doc is generated for a base sync.
     * @param modelClass Class for which we want to sync.
     * @param lastSync The last time synced. If not provided, do a base query.
     *                 If provided, do a delta query.
     * @param <T> The type of objects we are syncing
     * @return A string which contains a GraphQL query doc for an base/delta sync
     * @throws DataStoreException On Failure to inspect
     */
    @NonNull
    static <T, M extends Model> AppSyncGraphQLRequest<T> buildSyncRequest(
            @NonNull final Class<M> modelClass,
            @Nullable final Long lastSync,
            @SuppressWarnings("SameParameterValue") @Nullable final String nextToken)
            throws DataStoreException {

        try {
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .modelClass(modelClass)
                    .operation(QueryType.SYNC)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .responseType(new TypeToken<Iterable<String>>(){}.getType());

            if (lastSync != null) {
                builder.variable("lastSync", "AWSTimestamp", lastSync);
            }

            if (nextToken != null) {
                builder.variable("nextToken", "String", nextToken);
            }

            return builder.build();
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <T, M extends Model> AppSyncGraphQLRequest<T> buildSubscriptionRequest(
            Class<M> modelClass, SubscriptionType subscriptionType) throws DataStoreException {
        try {
            return AppSyncGraphQLRequest.builder()
                    .modelClass(modelClass)
                    .operation(subscriptionType)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .responseType(String.class)
                    .build();
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <T extends Model> String buildDeletionDoc(Class<T> modelClass, boolean includePredicate)
        throws DataStoreException {
        return buildMutation(modelClass, includePredicate, MutationType.DELETE);
    }

    static <T extends Model> String buildUpdateDoc(Class<T> modelClass, boolean includePredicate)
        throws DataStoreException {
        return buildMutation(modelClass, includePredicate, MutationType.UPDATE);
    }

    static <T extends Model> String buildCreationDoc(Class<T> modelClass) throws DataStoreException {
        return buildMutation(modelClass, false, MutationType.CREATE);
    }

    static Map<String, Object> parsePredicate(QueryPredicate queryPredicate) throws DataStoreException {
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation<?> qpo = (QueryPredicateOperation) queryPredicate;
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
                    throw new DataStoreException(
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
            throw new DataStoreException(
                "Tried to parse an unsupported QueryPredicate",
                "Try changing to one of the supported values: QueryPredicateOperation, QueryPredicateGroup."
            );
        }
    }

    private static String appSyncOpType(QueryOperator.Type type) throws DataStoreException {
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
                throw new DataStoreException(
                    "Tried to parse an unsupported QueryOperator type",
                    "Check if a new QueryOperator.Type enum has been created which is not supported " +
                        "in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    private static Object appSyncOpValue(QueryOperator<?> qOp) throws DataStoreException {
        switch (qOp.type()) {
            case NOT_EQUAL:
                return ((NotEqualQueryOperator) qOp).value();
            case EQUAL:
                return ((EqualQueryOperator) qOp).value();
            case LESS_OR_EQUAL:
                return ((LessOrEqualQueryOperator) qOp).value();
            case LESS_THAN:
                return ((LessThanQueryOperator) qOp).value();
            case GREATER_OR_EQUAL:
                return ((GreaterOrEqualQueryOperator) qOp).value();
            case GREATER_THAN:
                return ((GreaterThanQueryOperator) qOp).value();
            case CONTAINS:
                return ((ContainsQueryOperator) qOp).value();
            case BETWEEN:
                BetweenQueryOperator<?> betweenOp = (BetweenQueryOperator) qOp;
                return Arrays.asList(betweenOp.start(), betweenOp.end());
            case BEGINS_WITH:
                return ((BeginsWithQueryOperator) qOp).value();
            default:
                throw new DataStoreException(
                    "Tried to parse an unsupported QueryOperator type",
                    "Check if a new QueryOperator.Type enum has been created which is not supported " +
                        "in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    /**
     * Builds a mutation.
     * @param modelClass class of the model
     * @param mutationType Type of mutation, e.g. {@link MutationType#CREATE}
     * @param <T> Type of model being mutated
     * @return Mutation doc
     */
    private static <T extends Model> String buildMutation(Class<T> modelClass,
                                                          boolean includePredicate,
                                                          MutationType mutationType) throws DataStoreException {
        final String capitalizedModelName = Casing.capitalizeFirst(modelClass.getSimpleName());
        int indent = 0;
        StringBuilder doc = new StringBuilder();

        final String verb;
        switch (mutationType) {
            case CREATE:
                verb = "Create";
                break;
            case UPDATE:
                verb = "Update";
                break;
            case DELETE:
                verb = "Delete";
                break;
            default:
                throw new DataStoreException(
                    "Unknown mutation type = " + mutationType,
                    "Check if support has been added in the sync engine."
                );
        }

        // mutation CreateBlogOwner($input:CreateBlogOwnerInput!) {
        doc.append("mutation ").append(verb).append(capitalizedModelName)
            .append("($input:").append(verb).append(capitalizedModelName).append("Input!");

        if (includePredicate) {
            doc.append(", $condition:Model")
                .append(capitalizedModelName)
                .append("ConditionInput");
        }

        //   createBlogOwner(input:$input)
        doc.append(") {\n")
            .append(padBy(++indent)).append(verb.toLowerCase(Locale.US)).append(capitalizedModelName)
            .append("(input:$input");

        if (includePredicate) {
            doc.append(", condition:$condition");
        }

        doc.append(") {\n");

        ++indent;
        doc.append(buildSelectionPortion(modelClass, indent, WALK_DEPTH));
        for (final String itemSyncKey : ITEM_SYNC_KEYS) {
            doc.append(padBy(indent)).append(itemSyncKey).append("\n");
        }
        --indent;

        // end the the inner createWhatever directive
        doc.append(padBy(indent)).append("}\n");

        // End the container (that started as `mutation CreateBlogOwner(` etc.
        doc.append(padBy(--indent)).append("}\n");

        return doc.toString();
    }

    private static String padBy(int indent) {
        if (indent <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < indent; index++) {
            builder.append("  "); // 2 spaces per level
        }
        return builder.toString();
    }

    private static <T extends Model> String buildSelectionPortion(
            final Class<T> modelClass,
            final int indentationLevel,
            @SuppressWarnings("SameParameterValue") final int levelsToGo)
            throws DataStoreException {

        if (levelsToGo < 0) {
            return "\n";
        }

        int indents = indentationLevel;
        final StringBuilder result = new StringBuilder();
        ModelSchema schema;
        try {
            schema = ModelSchema.fromModelClass(modelClass);
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                amplifyException, "Validate your model file.");
        }

        for (Field field : FieldFinder.findFieldsIn(modelClass)) {
            if (schema.getAssociations().containsKey(field.getName())) {
                if (List.class.isAssignableFrom(field.getType()) && levelsToGo > 0) {
                    result
                        .append(padBy(indents)).append(field.getName()).append(" {\n")
                        .append(padBy(++indents)).append("items {\n")
                        .append(padBy(++indents)).append("id\n")
                        .append(padBy(--indents)).append("}\n")
                        .append(padBy(indents)).append("nextToken\n")
                        .append(padBy(indents)).append("startedAt\n")
                        .append(padBy(--indents)).append("}\n");
                } else if (levelsToGo > 0) {
                    result
                        .append(padBy(indents)).append(field.getName()).append(" {\n")
                        .append(padBy(++indents)).append("id\n")
                        .append(padBy(--indents)).append("}\n");
                }
            } else {
                result.append(padBy(indents)).append(field.getName()).append("\n");
            }
        }
        return result.toString();
    }
}
