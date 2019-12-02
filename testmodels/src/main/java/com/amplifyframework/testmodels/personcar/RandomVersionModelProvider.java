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

package com.amplifyframework.testmodels;

import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An implementation of ModelProvider that produces a random string version
 * to test model version changes.
 */
public final class RandomVersionModelProvider implements ModelProvider {

    private static RandomVersionModelProvider singleton;

    private final String version;

    private RandomVersionModelProvider() {
        this.version = UUID.randomUUID().toString();
    }

    /**
     * Gets the singleton instance of the {@link RandomVersionModelProvider}.
     * @return Singleton of the Model provider
     */
    public static synchronized RandomVersionModelProvider singletonInstance() {
        if (singleton == null) {
            singleton = new RandomVersionModelProvider();
        }
        return singleton;
    }

    /**
     * Get a set of the model classes.
     *
     * @return a set of the model classes.
     */
    @Override
    public Set<Class<? extends Model>> models() {
        final Set<Class<? extends Model>> modifiableSet = new HashSet<>();
        modifiableSet.add(Person.class);
        modifiableSet.add(Car.class);
        return Immutable.of(modifiableSet);
    }

    /**
     * Get the version of the models.
     *
     * @return the version string of the models.
     */
    @Override
    public String version() {
        return version;
    }
}
