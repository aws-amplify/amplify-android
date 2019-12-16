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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.util.Immutable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class ModelProviderFactory {
    @SuppressWarnings("checkstyle:all") private ModelProviderFactory() {}

    @SafeVarargs
    @SuppressWarnings("varargs")
    static ModelProvider including(final Class<? extends Model>... modelClasses) {
        final Set<Class<? extends Model>> models = new HashSet<>(Arrays.asList(modelClasses));
        return new SimpleModelProvider(UUID.randomUUID().toString(), models);
    }

    static final class SimpleModelProvider implements ModelProvider {
        private final String version;
        private final Set<Class<? extends Model>> models;

        SimpleModelProvider(final String version, final Set<Class<? extends Model>> models) {
            this.version = version;
            this.models = models;
        }

        @Override
        public Set<Class<? extends Model>> models() {
            return Immutable.of(models);
        }

        @Override
        public String version() {
            return version;
        }
    }
}
