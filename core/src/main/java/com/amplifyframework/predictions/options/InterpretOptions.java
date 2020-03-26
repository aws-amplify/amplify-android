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

package com.amplifyframework.predictions.options;

import com.amplifyframework.core.async.Options;

/**
 * Options for text interpretation operation.
 */
public final class InterpretOptions implements Options {

    private InterpretOptions() {
        // TODO: Add options
    }

    /**
     * Creates an instance of options with default values assigned.
     * @return Default instance of options
     */
    public static InterpretOptions defaultInstance() {
        return new InterpretOptions();
    }
}
