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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Gson adapters to serialize/deserialize to/from data modeling types.
 */
public final class GsonPredicateAdapters {
    private GsonPredicateAdapters() {}

    /**
     * Registers the QueryPredicate adapter into an {@link GsonBuilder}.
     * registerTypeHierarchyAdapter enables objects assigned to concrete QueryPredicate classes
     * (e.g., QueryPredicateOperation) to use this adapter.
     * 
     * @param builder A GsonBuilder.
     */
    public static void register(GsonBuilder builder) {
        builder
            .registerTypeHierarchyAdapter(QueryPredicate.class, new QueryPredicateAdapter());
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

        // internal Gson instance for avoiding infinite loop
        private final Gson gson = new GsonBuilder()
                .registerTypeAdapter(QueryOperator.class, new QueryOperatorAdapter())
                .serializeNulls()
                .create();

        private enum PredicateType {
            OPERATION,
            GROUP,
            ALL,
            NONE
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
                    return gson.fromJson(json, QueryPredicateOperation.class);
                case GROUP:
                    // We need to manually deserialize Groups to ensure we handle nested groups
                    // and update _types correctly.
                    return deserializeQueryPredicateGroup(jsonObject);
                case ALL:
                    return gson.fromJson(json, MatchAllQueryPredicate.class);
                case NONE:
                    return gson.fromJson(json, MatchNoneQueryPredicate.class);
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
            if (predicate instanceof QueryPredicateGroup) {
                // We need to manually serialize Groups to ensure we handle nested groups and
                // update _types correctly.
                predicateType = PredicateType.GROUP;
                json = serializeQueryPredicateGroup((QueryPredicateGroup) predicate, context);
            } else {
                json = gson.toJsonTree(predicate);
                if (predicate instanceof MatchAllQueryPredicate) {
                    predicateType = PredicateType.ALL;
                } else if (predicate instanceof MatchNoneQueryPredicate) {
                    predicateType = PredicateType.NONE;
                } else if (predicate instanceof QueryPredicateOperation) {
                    predicateType = PredicateType.OPERATION;
                } else {
                    throw new JsonParseException("Unable to identify the predicate type.");
                }
            }
            JsonObject jsonObject = json.getAsJsonObject();
            jsonObject.addProperty(TYPE, predicateType.name());
            return jsonObject;
        }

        /**
         * Serializes a QueryPredicateGroup to JSON format.
         * <p>
         * This method is necessary because QueryPredicateGroup contains nested QueryPredicate objects
         * that need to be recursively serialized. We cannot use context.serialize() directly on
         * QueryPredicateGroup because:
         * 1. Using context.serialize() would cause infinite recursion back to this adapter
         * 2. We need to manually construct the JSON structure with proper "_type" fields
         * <p>
         * The method handles:
         * - Serializing the group type (AND, OR, NOT)
         * - Recursively serializing each nested predicate in the predicates array
         * - Maintaining the correct JSON structure expected by the deserializer
         * 
         * @param group The QueryPredicateGroup to serialize
         * @param context The serialization context for handling nested QueryOperator objects
         * @return JsonElement representing the serialized group
         */
        private JsonElement serializeQueryPredicateGroup(QueryPredicateGroup group, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", group.type().name());
            
            JsonArray predicatesArray = new JsonArray();
            for (QueryPredicate predicate : group.predicates()) {
                // Recursively serialize nested predicates using this adapter
                predicatesArray.add(serialize(predicate, QueryPredicate.class, context));
            }
            jsonObject.add("predicates", predicatesArray);
            
            return jsonObject;
        }
        
        /**
         * Deserializes a JSON object into a QueryPredicateGroup.
         * <p>
         * This method is necessary because QueryPredicateGroup contains nested QueryPredicate objects
         * that need to be recursively deserialized. We cannot use context.deserialize() directly
         * because:
         * 1. Using context.deserialize() would cause infinite recursion back to this adapter
         * 2. We need to manually parse the JSON structure and handle nested predicates
         * <p>
         * The method handles:
         * - Parsing the group type (AND, OR, NOT) from the "type" field
         * - Recursively deserializing each predicate in the "predicates" array
         * - Creating the QueryPredicateGroup with the correct constructor (no builder available)
         * <p>
         * This is critical for DataStore sync expressions that contain nested predicate groups,
         * which caused the "Interfaces can't be instantiated" error before this fix.
         * 
         * @param jsonObject The JSON object containing the group data
         * @return QueryPredicateGroup instance with all nested predicates deserialized
         */
        private QueryPredicateGroup deserializeQueryPredicateGroup(JsonObject jsonObject) {
            QueryPredicateGroup.Type type = QueryPredicateGroup.Type.valueOf(
                jsonObject.get("type").getAsString()
            );
            
            List<QueryPredicate> predicates = new ArrayList<>();
            JsonArray predicatesArray = jsonObject.getAsJsonArray("predicates");
            for (JsonElement predicateElement : predicatesArray) {
                // Recursively deserialize nested predicates using this adapter
                // Note: Passing null for context since we handle recursion manually
                QueryPredicate predicate = deserialize(predicateElement, QueryPredicate.class, null);
                predicates.add(predicate);
            }
            
            // Use constructor since QueryPredicateGroup doesn't have a builder
            return new QueryPredicateGroup(type, predicates);
        }
    }
}
