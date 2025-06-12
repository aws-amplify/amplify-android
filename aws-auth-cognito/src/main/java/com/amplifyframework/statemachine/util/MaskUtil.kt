/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.statemachine.util

internal fun String?.mask() = if (this == null || this.length <= 4) {
    "***"
} else {
    "${this.substring(0 until 4)}***"
}

/**
 * Masks the values of the given keys in the map, while leaving other values unmasked
 */
internal fun Map<String, String>.mask(vararg keys: String): Map<String, String> = mapValues { (key, value) ->
    if (keys.contains(key)) value.mask() else value
}
