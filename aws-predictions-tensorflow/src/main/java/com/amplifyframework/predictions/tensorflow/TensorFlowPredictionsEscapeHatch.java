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

package com.amplifyframework.predictions.tensorflow;

import androidx.annotation.NonNull;

import com.amplifyframework.util.Immutable;

import org.tensorflow.lite.Interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * An escape hatch to give low-level access to TensorFlow interpreter.
 */
public final class TensorFlowPredictionsEscapeHatch {
    private final Map<String, Interpreter> interpreters;

    TensorFlowPredictionsEscapeHatch(@NonNull Map<String, Interpreter> interpreters) {
        this.interpreters = new HashMap<>();

        // Only insert non-null interpreters from the map
        for (String service : interpreters.keySet()) {
            final Interpreter interpreter = interpreters.get(service);
            if (interpreter != null) {
                this.interpreters.put(service, interpreter);
            }
        }
    }

    /**
     * Return a map of pre-trained TensorFlow Lite interpreters
     * used by the plugin.
     * @return the map of {service key -> interpreter}
     */
    public Map<String, Interpreter> getInterpreters() {
        return Immutable.of(interpreters);
    }
}
