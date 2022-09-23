package com.amplifyframework.datastore.model;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.amplifyframework.core.model.InMemoryLazyModel;
import com.amplifyframework.core.model.LazyModel;
import com.amplifyframework.core.model.Model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class LazyModelAdapter<M extends Model> implements JsonDeserializer<LazyModel<M>>,
        JsonSerializer<LazyModel<M>> {


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public LazyModel<M> deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context) throws JsonParseException {
        ParameterizedType pType = (ParameterizedType) typeOfT;
        Type type = (Class<?>) pType.getActualTypeArguments()[0];

        Log.d("LazyModelAdapter", "json: "+ json + " typeOfT " + typeOfT +
                " typeOfT type name" + type + " context " +
                context);
        return new InMemoryLazyModel<M>(context.deserialize(json, type));
    }

    @Override
    public JsonElement serialize(LazyModel<M> src, Type typeOfSrc,
                                 JsonSerializationContext context) {
        return null;
    }
}