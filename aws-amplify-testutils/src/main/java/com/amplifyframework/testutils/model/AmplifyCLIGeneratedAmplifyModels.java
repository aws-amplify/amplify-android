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

package com.amplifyframework.testutils.model;

import com.amplifyframework.core.model.AmplifyModels;
import com.amplifyframework.core.model.Model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains the set of model classes that implement {@link Model}
 * interface.
 */
public final class AmplifyCLIGeneratedAmplifyModels implements AmplifyModels {

    private static final String AMPLIFY_MODELS_VERSION = "hash-code";
    private static AmplifyCLIGeneratedAmplifyModels amplifyCLIGeneratedAmplifyModelsInstance;

    private AmplifyCLIGeneratedAmplifyModels() {
    }

    public static synchronized AmplifyCLIGeneratedAmplifyModels getInstance() {
        if (amplifyCLIGeneratedAmplifyModelsInstance == null) {
            amplifyCLIGeneratedAmplifyModelsInstance = new AmplifyCLIGeneratedAmplifyModels();
        }
        return amplifyCLIGeneratedAmplifyModelsInstance;
    }

    /**
     * Get a set of the model classes.
     *
     * @return a set of the model classes.
     */
    @Override
    public Set<Class<? extends Model>> list() {
        return new HashSet<Class<? extends Model>>(Arrays.asList(
                Person.class
        ));
    }

    /**
     * Get the version of the models.
     *
     * @return the version string of the models.
     */
    @Override
    public String version() {
        return AMPLIFY_MODELS_VERSION;
    }
}
