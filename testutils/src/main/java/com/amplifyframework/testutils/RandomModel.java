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

package com.amplifyframework.testutils;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;

/**
 * A test utility to generate random instances of {@link Model}s.
 */
public final class RandomModel {
    @SuppressWarnings("checkstyle:all") private RandomModel() {}

    /**
     * Creates a random instance of an {@link Model}.
     * @return A random Model object
     */
    @NonNull
    public static Model model() {
        return new SimpleModel(RandomString.string());
    }

    private static class SimpleModel implements Model {
        private final String modelId;

        SimpleModel(String modelId) {
            this.modelId = modelId;
        }

        @Override
        public String getId() {
            return modelId;
        }
    }
}
