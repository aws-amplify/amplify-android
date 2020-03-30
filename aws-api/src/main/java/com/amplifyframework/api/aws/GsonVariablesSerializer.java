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

package com.amplifyframework.api.aws;

import com.amplifyframework.api.graphql.GraphQLRequest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of a GraphQL Request serializer for the variables map using Gson.
 */
public final class GsonVariablesSerializer implements GraphQLRequest.VariablesSerializer {
    @Override
    public String serialize(Map<String, Object> variables) {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateSerializer())
                .create()
                .toJson(variables);
    }

    class DateSerializer implements JsonSerializer<Date> {
        public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return new JsonPrimitive(df.format(date));
        }
    }
}
