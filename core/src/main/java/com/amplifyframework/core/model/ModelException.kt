/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model

import com.amplifyframework.AmplifyException

sealed class ModelException(
    message: String,
    recoverySuggestion: String,
    cause: Exception? = null
) : AmplifyException(message, cause, recoverySuggestion) {

    class PropertyPathNotFound(
        val modelName: String,
        cause: Exception? = null
    ) : ModelException(
        "The root property path for the model $modelName could not be found",
        "Check if the model types were generated with the latest Amplify CLI and try again",
        cause
    )
}
