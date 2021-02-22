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

package com.amplifyframework.core.model.query.predicate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Gson adapters to serialize/deserialize to/from data modeling types.
 */
public final class GsonPredicateAdapters {
    private GsonPredicateAdapters() {}

    /**
     * Registers the adapters into an {@link GsonBuilder}.
     * @param builder A GsonBuilder.
     */
    public static void register(GsonBuilder builder) {
        builder
            .registerTypeAdapter(QueryOperator.class, new QueryOperatorAdapter())
            .registerTypeAdapter(QueryPredicate.class, new QueryPredicateAdapter());
    }

    /**
     * Custom logic to serialize and deserialize an instance of {@link QueryOperator}.
     */
    public static final class QueryOperatorAdapter implements
            JsonDeserializer<QueryOperator<?>>, JsonSerializer<QueryOperator<?>> {
        private static final String TYPE = "type";

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryOperator<?> deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            JsonObject jsonObject = json.getAsJsonObject();
            String operatorType = jsonObject.get(TYPE).getAsString();
            switch (QueryOperator.Type.valueOf(operatorType)) {
                case CONTAINS:
                    return context.deserialize(json, ContainsQueryOperator.class);
                case NOT_CONTAINS:
                    return context.deserialize(json, NotContainsQueryOperator.class);
                case GREATER_OR_EQUAL:
                    return context.deserialize(json, GreaterOrEqualQueryOperator.class);
                case LESS_OR_EQUAL:
                    return context.deserialize(json, LessOrEqualQueryOperator.class);
                case GREATER_THAN:
                    return context.deserialize(json, GreaterThanQueryOperator.class);
                case LESS_THAN:
                    return context.deserialize(json, LessThanQueryOperator.class);
                case BETWEEN:
                    return context.deserialize(json, BetweenQueryOperator.class);
                case EQUAL:
                    return context.deserialize(json, EqualQueryOperator.class);
                case NOT_EQUAL:
                    return context.deserialize(json, NotEqualQueryOperator.class);
                case BEGINS_WITH:
                    return context.deserialize(json, BeginsWithQueryOperator.class);
                default:
                    throw new JsonParseException("Unable to deserialize " +
                            json.toString() + " to QueryOperator instance.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonElement serialize(QueryOperator<?> operator, Type type, JsonSerializationContext context) {
            if (operator instanceof ContainsQueryOperator) {
                return context.serialize(operator, ContainsQueryOperator.class);
            } else if (operator instanceof NotContainsQueryOperator) {
                return context.serialize(operator, NotContainsQueryOperator.class);
            } else if (operator instanceof GreaterOrEqualQueryOperator) {
                return context.serialize(operator, GreaterOrEqualQueryOperator.class);
            } else if (operator instanceof LessOrEqualQueryOperator) {
                return context.serialize(operator, LessOrEqualQueryOperator.class);
            } else if (operator instanceof GreaterThanQueryOperator) {
                return context.serialize(operator, GreaterThanQueryOperator.class);
            } else if (operator instanceof LessThanQueryOperator) {
                return context.serialize(operator, LessThanQueryOperator.class);
            } else if (operator instanceof BetweenQueryOperator) {
                return context.serialize(operator, BetweenQueryOperator.class);
            } else if (operator instanceof EqualQueryOperator) {
                return context.serialize(operator, EqualQueryOperator.class);
            } else if (operator instanceof NotEqualQueryOperator) {
                return context.serialize(operator, NotEqualQueryOperator.class);
            } else if (operator instanceof BeginsWithQueryOperator) {
                return context.serialize(operator, BeginsWithQueryOperator.class);
            } else {
                throw new JsonParseException("Unable to serialize a QueryOperator " +
                        "of type " + operator.type().name() + ".");
            }
        }
    }

    /**
     * Custom logic to serialize and deserialize an instance of {@link QueryPredicate}.
     */
    public static final class QueryPredicateAdapter implements
            JsonDeserializer<QueryPredicate>, JsonSerializer<QueryPredicate> {
        private static final String TYPE = "_type";

        private enum PredicateType {
            OPERATION,
            GROUP,
            ALL
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryPredicate deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }

            JsonObject jsonObject = json.getAsJsonObject();
            String predicateType = jsonObject.get(TYPE).getAsString();
            switch (PredicateType.valueOf(predicateType)) {
                case OPERATION:
                    return context.deserialize(json, QueryPredicateOperation.class);
                case GROUP:
                    return context.deserialize(json, QueryPredicateGroup.class);
                case ALL:
                    return context.deserialize(json, MatchAllQueryPredicate.class);
                default:
                    throw new JsonParseException("Unable to deserialize " +
                            json.toString() + " to QueryPredicate instance.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonElement serialize(QueryPredicate predicate, Type type, JsonSerializationContext context)
                throws JsonParseException {
            JsonElement json;
            PredicateType predicateType;
            if (predicate instanceof MatchAllQueryPredicate) {
                predicateType = PredicateType.ALL;
                json = context.serialize(predicate, MatchAllQueryPredicate.class);
            } else if (predicate instanceof QueryPredicateOperation) {
                json = context.serialize(predicate, QueryPredicateOperation.class);
                predicateType = PredicateType.OPERATION;
            } else if (predicate instanceof QueryPredicateGroup) {
                json = context.serialize(predicate, QueryPredicateGroup.class);
                predicateType = PredicateType.GROUP;
            } else {
                throw new JsonParseException("Unable to identify the predicate type.");
            }
            JsonObject jsonObject = json.getAsJsonObject();
            jsonObject.addProperty(TYPE, predicateType.name());
            return jsonObject;
        }
    }
}
