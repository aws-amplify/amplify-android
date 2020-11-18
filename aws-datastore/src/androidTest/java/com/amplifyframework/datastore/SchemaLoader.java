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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility to load {@link ModelSchema} from JSON files in the ./assets directory.
 * Returns them in a {@link SchemaProvider}.
 */
final class SchemaLoader {
    private SchemaLoader() {}

    /**
     * Creates an {@link SchemaProvider} by loading schema JSON files from the
     * ./assets directory.
     * @param assetsDirectoryPath Directory under ./assets, e.g., "schemas/commentsblog".
     * @return A SchemaProvider that vends {@link ModelSchema}, each of which was derived
     *         from a schema JSON
     */
    @NonNull
    static SchemaProvider loadFromAssetsDirectory(
            @SuppressWarnings("SameParameterValue") String assetsDirectoryPath) {
        List<ModelSchema> schemas = new ArrayList<>();
        Gson gson = GsonFactory.instance();
        for (String fileName : Assets.list(assetsDirectoryPath)) {
            schemas.add(gson.fromJson(Assets.readAsString(fileName), ModelSchema.class));
        }
        return SchemaProvider.of(schemas.toArray(new ModelSchema[0]));
    }
}
