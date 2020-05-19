package com.amplifyframework.api.aws;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.Page;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Takes an input such as the following and deserializes to an AppSyncPage
 * {
 *   "items": [
 *     {
 *       "description": null,
 *       "id": "92863611-684a-424d-b3e5-94d42c4914c9",
 *       "name": "some task"
 *     }
 *   ],
 *   "nextToken": "asdf"
 * }
 */
class AppSyncPageDeserializer implements JsonDeserializer<Page<Object>> {
    private static final String ITEMS_KEY = "items";
    private static final String NEXT_TOKEN_KEY = "nextToken";

    private final GraphQLRequest<Page<Object>> request;

    public AppSyncPageDeserializer(GraphQLRequest<Page<Object>> request) {
        this.request = request;
    }

    @Override
    @SuppressWarnings("unchecked") // Cast Type to Class<Object>
    public Page<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final Class<Object> templateClassType;
        if (typeOfT instanceof ParameterizedType) {
            // Because typeOfT is ParameterizedType we can be sure this is a safe cast.
            templateClassType = (Class<Object>) ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
        } else {
            throw new JsonParseException("Expected a parameterized type during AppSyncPage deserialization.");
        }
        if(!json.isJsonObject()) {
            throw new JsonParseException("Expected JsonObject while deserializing AppSyncPage but found " + json);
        }
        JsonObject jsonObject = json.getAsJsonObject();

        Type dataType = TypeToken.getParameterized(Iterable.class, templateClassType).getType();
        Iterable<Object> items = context.deserialize(jsonObject.get(ITEMS_KEY), dataType);

        JsonElement nextTokenElement = jsonObject.get(NEXT_TOKEN_KEY);
        GraphQLRequest<Page<Object>> requestForNextPage = null;
        if(nextTokenElement.isJsonPrimitive()) {
            String nextToken = nextTokenElement.getAsJsonPrimitive().getAsString();
            requestForNextPage = request.copy();
            requestForNextPage.putVariable(NEXT_TOKEN_KEY, nextToken);
        }

        return new AppSyncPage<>(items, requestForNextPage);
    }
}
