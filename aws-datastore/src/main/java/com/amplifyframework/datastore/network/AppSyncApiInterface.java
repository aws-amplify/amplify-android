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

package com.amplifyframework.datastore.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.util.FieldFinder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of API Interface for AppSync
 */
public final class AppSyncApiInterface implements ApiInterface {
    private static final int DEFAULT_QUERY_LIMIT = 1000;
    private static final int DEFAULT_LEVEL_DEPTH = 2;

    @Override
    public <T extends Model> GraphQLOperation<T> sync(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync,
            @NonNull ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> create(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> update(
            @NonNull String apiName,
            @NonNull T model,
            @NonNull Integer version,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> delete(
            @NonNull String apiName,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> onCreate(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> onUpdate(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> onDelete(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener
    ) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String getModelFields(Class<? extends Model> clazz, int levelsDeepToGo) throws AmplifyException {
        if (levelsDeepToGo < 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        ModelSchema schema = ModelSchema.fromModelClass(clazz);
        Iterator<Field> iterator = FieldFinder.findFieldsIn(clazz).iterator();

        while (iterator.hasNext()) {
            Field field = iterator.next();
            String fieldName = field.getName();

            if (schema.getAssociations().containsKey(fieldName)) {
                if (List.class.isAssignableFrom(field.getType())) {
                    if (levelsDeepToGo >= 1) {
                        result.append(fieldName).append(" ");

                        ParameterizedType listType = (ParameterizedType) field.getGenericType();
                        Class<Model> listTypeClass = (Class<Model>) listType.getActualTypeArguments()[0];

                        result.append("{ items {")
                                .append(getModelFields(listTypeClass, levelsDeepToGo - 1)) // cast checked above
                                .append(" _version _deleted _lastChangedAt")
                                .append("} nextToken }");
                    }
                } else if (levelsDeepToGo >= 1) {
                    result.append(fieldName).append(" ");

                    result.append("{")
                            .append(getModelFields((Class<Model>) field.getType(), levelsDeepToGo - 1))
                            .append("}");
                }
            } else {
                result.append(fieldName).append(" ");
            }
        }

        return result.toString();
    }
}
