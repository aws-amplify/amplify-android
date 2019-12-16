package com.amplifyframework.datastore.storage;

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
final class PredicateInterfaceAdapter implements
        JsonDeserializer<QueryPredicate>,
        JsonSerializer<QueryPredicate> {

    private static final String TYPE = "_type";

    private enum PredicateType {
        OPERATION,
        GROUP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryPredicate deserialize(JsonElement json, Type type,
                         JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String predicateType = jsonObject.get(TYPE).getAsString();
        switch (PredicateType.valueOf(predicateType)) {
            case OPERATION:
                return context.deserialize(json, QueryPredicateOperation.class);
            case GROUP:
                return context.deserialize(json, QueryPredicateGroup.class);
            default:
                throw new JsonParseException("Unable to deserialize to QueryPredicate.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonElement serialize(QueryPredicate predicate, Type type, JsonSerializationContext context) {
        JsonElement json;
        PredicateType predicateType;
        if (predicate instanceof QueryPredicateOperation) {
            json = context.serialize(predicate, QueryPredicateOperation.class);
            predicateType = PredicateType.OPERATION;
        } else if (predicate instanceof QueryPredicateGroup) {
            json = context.serialize(predicate, QueryPredicateGroup.class);
            predicateType = PredicateType.GROUP;
        } else {
            throw new JsonParseException("Unable to serialize this instance of QueryPredicate.");
        }
        JsonObject jsonObject = json.getAsJsonObject();
        jsonObject.addProperty(TYPE, predicateType.name());
        return jsonObject;
    }
}
