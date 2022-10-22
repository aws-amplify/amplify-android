package com.amplifyframework.api.aws;

import android.util.Log;

import com.amplifyframework.core.model.LazyList;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.SchemaRegistry;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LazyModelListAdapter<M extends Model> implements JsonDeserializer<LazyList<M>>,
        JsonSerializer<LazyList<M>> {

    @SuppressWarnings("unchecked")
    @Override
    public LazyList<M> deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {
        ParameterizedType pType = (ParameterizedType) typeOfT;
        Class<M> type = (Class<M>) pType.getActualTypeArguments()[0];

        Log.d("LazyModelAdapter", "json: "+ json + " typeOfT " + typeOfT +
                " typeOfT type name" + type + " context " +
                context);
         Map<String, Object> predicateKeyMap = new HashMap<>();
        Iterator<String> primaryKeysIterator = SchemaRegistry.instance()
                .getModelSchemaForModelClass(type)
                .getPrimaryIndexFields().iterator();
        JsonObject jsonObject = (JsonObject) json;
        while (primaryKeysIterator.hasNext()){
            String primaryKey = primaryKeysIterator.next();
            predicateKeyMap.put(primaryKey, jsonObject.get(primaryKey));
        }
        return new AppSyncLazyListModel<>(type, predicateKeyMap, new AppSyncLazyQueryPredicate<>());
    }

    @Override
    public JsonElement serialize(LazyList<M> src, Type typeOfSrc,
                                 JsonSerializationContext context) {
        return null;
    }
}