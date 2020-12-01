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

package com.amplifyframework.core.model.types;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Gson adapters to serialize/deserialize Java types to/from JSON.
 */
public final class GsonJavaTypeAdapters {
    private GsonJavaTypeAdapters() {}

    /**
     * Registers the type adapters into a {@link GsonBuilder instance}.
     * @param builder GsonBuilder instance
     */
    public static void register(@NonNull GsonBuilder builder) {
        builder
            .registerTypeAdapter(String.class, new StringDeserializer())
            .registerTypeAdapterFactory(new ClassTypeAdapterFactory());
    }

    /**
     * Normally Gson will only deserialize a JsonPrimitive into a String.  This deserializer enables JsonObject to be
     * serialized to a String, instead of throwing an exception.
     */
    public static final class StringDeserializer implements JsonDeserializer<String> {
        @Override
        public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return json.getAsJsonPrimitive().getAsString();
            } else if (json.isJsonObject()) {
                return json.toString();
            } else {
                throw new JsonParseException("Failed to parse String from " + json);
            }
        }
    }

    /**
     * A {@link TypeAdapterFactory} which generates a {@link TypeAdapter} for use
     * with {@link Class}-type objects.
     */
    public static final class ClassTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked") // (TypeAdapter<T>)
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (!Class.class.isAssignableFrom(typeToken.getRawType())) {
                return null;
            }
            return (TypeAdapter<T>) new ClassTypeAdapter();
        }

        /**
         * DataStore's PendingMutations contains an {@link Class} member, but Gson doesn't
         * know what to do with it. So, we need this custom {@link TypeAdapter}.
         */
        static final class ClassTypeAdapter extends TypeAdapter<Class<?>> {
            @Override
            public void write(JsonWriter jsonWriter, Class<?> clazz) throws IOException {
                if (clazz == null) {
                    jsonWriter.nullValue();
                    return;
                }
                jsonWriter.value(clazz.getName());
            }

            @Override
            public Class<?> read(JsonReader jsonReader) throws IOException {
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    return null;
                }
                String classString = jsonReader.nextString();
                try {
                    return Class.forName(classString);
                } catch (ClassNotFoundException exception) {
                    // Class.forName() doesn't support primitives.
                    // So, we'll try that next, before giving up.
                }
                try {
                    return boxForPrimitiveLabel(classString);
                } catch (IllegalArgumentException illegalArgumentException) {
                    // At this point, we've tried to load the class,
                    // and also tried to box up a primitive, but neither have worked.
                    throw new IOException("Unable to deserialize class for " + classString);
                }
            }

            private static Class<?> boxForPrimitiveLabel(String primitiveLabel) {
                switch (primitiveLabel) {
                    case "boolean":
                        return Boolean.class;
                    case "byte":
                        return Byte.class;
                    case "short":
                        return Short.class;
                    case "int":
                        return Integer.class;
                    case "long":
                        return Long.class;
                    case "float":
                        return Float.class;
                    case "double":
                        return Double.class;
                    case "char":
                        return Character.class;
                    case "void":
                        return Void.class;
                    default:
                        throw new IllegalArgumentException("No primitive with name = " + primitiveLabel);
                }
            }
        }
    }
}
