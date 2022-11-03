/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.analytics.pinpoint.targeting.endpointProfile

import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable

@Serializable
internal class EndpointProfileUser {
    private var userId: String? = null
    private var userAttributes: MutableMap<String, List<String>> = ConcurrentHashMap()
    fun getUserId(): String? {
        return userId
    }

    fun setUserId(userId: String?) {
        this.userId = userId
    }

    fun getUserAttributes(): Map<String, List<String>> {
        return userAttributes
    }

    fun setUserAttributes(userAttributes: MutableMap<String, List<String>>) {
        this.userAttributes = userAttributes
    }

    fun addUserAttribute(key: String, value: List<String>): EndpointProfileUser {
        userAttributes[key] = value
        return this
    }
}
