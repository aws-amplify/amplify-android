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

package com.amplifyframework.datastore;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class ModelProviderFactory {
    private ModelProviderFactory() {}

    @SafeVarargs
    static ModelProvider createProviderOf(Class<? extends Model>... models) {
        return new ModelProvider() {
            @Override
            public Set<Class<? extends Model>> models() {
                return new HashSet<>(Arrays.asList(models));
            }

            @Override
            public String version() {
                StringBuilder stringBuilder = new StringBuilder();
                for (Class<? extends Model> model : models) {
                    stringBuilder.append(":")
                        .append(model.getSimpleName());
                }
                return stringBuilder.toString();
            }
        };
    }
}
