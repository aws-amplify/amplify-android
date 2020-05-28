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

package com.amplifyframework.datastore.model;

import com.amplifyframework.core.model.query.predicate.MatchAllQueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Custom logic to serialize and deserialize an instance of {@link QueryPredicate}.
 */
public final class PredicateInterfaceAdapter implements
        JsonDeserializer<QueryPredicate>,
        JsonSerializer<QueryPredicate> {

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
    public QueryPredicate deserialize(
            JsonElement json,
            Type type,
            JsonDeserializationContext context
    ) throws JsonParseException {
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
    public JsonElement serialize(
            QueryPredicate predicate,
            Type type,
            JsonSerializationContext context
    ) throws JsonParseException {
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
