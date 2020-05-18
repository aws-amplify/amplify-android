package com.amplifyframework.api.aws;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Normally Gson will only deserialize a JsonPrimitive into a String.  This deserializer enables JsonObject to be
 * serialized to a String, instead of throwing an exception.
 */
class StringDeserializer implements JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return json.getAsJsonPrimitive().getAsString();
        } else if(json.isJsonObject()) {
            return json.toString();
        } else {
            throw new JsonParseException("Failed to parse String from " + json);
        }
    }
}
