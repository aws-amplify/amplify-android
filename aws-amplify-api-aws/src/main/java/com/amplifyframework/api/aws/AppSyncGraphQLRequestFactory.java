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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts provided model or class type into a request container
 * with automatically generated GraphQL documents that follow
 * AppSync specifications.
 */
final class AppSyncGraphQLRequestFactory {
    // This class should not be instantiated
    private AppSyncGraphQLRequestFactory() { }

    public static <T extends Model> GraphQLRequest<T> buildQuery(
            Class<T> modelClass,
            QueryPredicate predicate,
            QueryType type
    ) throws AmplifyException {
        StringBuilder doc = new StringBuilder();
        Map<String, Object> variables = new HashMap<>();
        ModelSchema schema = ModelSchema.fromModelClass(modelClass);
        String typeStr = type.toString();
        String modelName = schema.getTargetModelName();

        doc.append("query ")
                .append(StringUtils.capitalize(typeStr))
                .append(StringUtils.capitalize(modelName))
                .append("(");

        switch (type) {
            case GET:
                doc.append("$id: ID!) { get")
                        .append(StringUtils.capitalize(modelName))
                        .append("(id: $id) { ")
                        .append(getModelFields(schema))
                        .append("}}");

                try {
                    QueryPredicateOperation operation = (QueryPredicateOperation) predicate;
                    if (
                            schema.getFields().get(operation.field()).getTargetName().equals("id") &&
                                    operation.operator().type().equals(QueryOperator.Type.EQUAL)
                    ) {
                        variables.put("id", ((EqualQueryOperator) operation.operator()).value());
                    } else {
                        throw new AmplifyException(
                                "Invalid predicate supplied for GET query",
                                null,
                                "When calling a GET query, the predicate must be in the format of Model.ID.eq('value')",
                                false
                        );
                    }
                } catch (ClassCastException | NullPointerException exception) {
                    throw new AmplifyException(
                            "Invalid predicate supplied for GET query",
                            exception,
                            "When calling a GET query, the predicate must be in the format of Model.ID.eq('value')",
                            false
                    );
                }
                
                break;
            case LIST:
                doc.append("$filter: Model")
                        .append(StringUtils.capitalize(modelName))
                        .append("FilterInput ")
                        .append("$limit: Int $nextToken: String) { list")
                        .append(StringUtils.capitalize(modelName))
                        .append("s(filter: $filter, limit: $limit, nextToken: $nextToken) { items {")
                        .append(getModelFields(schema))
                        .append("} nextToken }}");
                break;
            default:
        }

        return new GraphQLRequest<>(
                doc.toString(),
                variables,
                modelClass,
                new GsonVariablesSerializer()
        );
    }

    @SuppressWarnings("unchecked")
    public static <T extends Model> GraphQLRequest<T> buildMutation(
            T model,
            QueryPredicate predicate,
            MutationType type
    ) throws AmplifyException {
        // model is of type T so this is a safe cast - hence the warning suppression
        Class<T> modelClass = (Class<T>) model.getClass();

        StringBuilder doc = new StringBuilder();
        ModelSchema schema = ModelSchema.fromModelClass(modelClass);
        String typeStr = type.toString();
        String modelName = schema.getTargetModelName();

        doc.append("mutation ")
            .append(StringUtils.capitalize(typeStr))
            .append(StringUtils.capitalize(modelName))
            .append("($input: ")
            .append(StringUtils.capitalize(typeStr))
            .append(StringUtils.capitalize(modelName))
            .append("Input!) { ")
            .append(typeStr.toLowerCase(Locale.getDefault()))
            .append(StringUtils.capitalize(modelName))
            .append("(input: $input) { ")
            .append(getModelFields(schema))
            .append("}}");

        Map<String, Object> input = new HashMap<>();

        if (type.equals(MutationType.DELETE)) {
            input.put("input", Collections.singletonMap("id", model.getId()));
        } else {
            input.put("input", schema.getMapOfFieldNameAndValues(model));
        }

        return new GraphQLRequest<>(
                doc.toString(),
                input,
                modelClass,
                new GsonVariablesSerializer()
        );
    }

    public static <T extends Model> GraphQLRequest<T> buildSubscription(
            Class<T> modelClass,
            QueryPredicate predicate,
            SubscriptionType type
    ) throws AmplifyException {
        return null;
    }

    private static String getModelFields(ModelSchema schema) {
        StringBuilder result = new StringBuilder();
        List<ModelField> sortedFields = schema.getSortedFields();

        for (int i = 0; i < sortedFields.size(); i++) {
            result.append(sortedFields.get(i).getTargetName());

            if (i < sortedFields.size() - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }
}
