/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.s3.utils

import com.google.gson.Gson

object JsonUtils {

    fun mapToString(input: Map<String, String>): String {
        return Gson().toJson(input).toString()
    }

    fun jsonToMap(input: String): Map<*, *> {
        return Gson().fromJson(input, Map::class.java)
    }
}