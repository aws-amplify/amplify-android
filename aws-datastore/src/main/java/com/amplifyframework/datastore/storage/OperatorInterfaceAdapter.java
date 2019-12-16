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

package com.amplifyframework.datastore.storage;

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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Custom logic to serialize and deserialize an instance of {@link QueryOperator}.
 */
final class OperatorInterfaceAdapter implements
        JsonDeserializer<QueryOperator>,
        JsonSerializer<QueryOperator> {

    private static final String TYPE = "type";

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryOperator deserialize(JsonElement json, Type type,
                         JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String operatorType = jsonObject.get(TYPE).getAsString();
        switch (QueryOperator.Type.valueOf(operatorType)) {
            case CONTAINS:
                return context.deserialize(json, ContainsQueryOperator.class);
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
                throw new JsonParseException("Unable to deserialize to QueryOperator.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonElement serialize(QueryOperator operator, Type type, JsonSerializationContext context) {
        if (operator instanceof ContainsQueryOperator) {
            return context.serialize(operator, ContainsQueryOperator.class);
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
            throw new JsonParseException("Unable to serialize this instance of QueryOperator.");
        }
    }
}
