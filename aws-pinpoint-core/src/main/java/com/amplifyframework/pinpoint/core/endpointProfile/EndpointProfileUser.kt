/*
 *  Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package com.amplifyframework.pinpoint.core.endpointProfile

import androidx.annotation.RestrictTo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EndpointProfileUser(val userId: String? = null) {
    val userAttributes: MutableMap<String, List<String>> = ConcurrentHashMap()

    fun addUserAttribute(key: String, value: List<String>): EndpointProfileUser {
        userAttributes[key] = value
        return this
    }
}
