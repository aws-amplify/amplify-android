/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.eventenrichment.metadata

/**
 * Device-level metadata stamped on every event.
 *
 * @param platform Platform name (e.g. "iOS", "Android").
 * @param platformVersion Platform OS version.
 * @param manufacturer Device manufacturer (e.g. "Apple", "Samsung").
 * @param model Device model (e.g. "iPhone", "SM-G900F").
 * @param locale Device locale code (e.g. "en_US").
 */
data class DeviceMetadata(
    val platform: String? = null,
    val platformVersion: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val locale: String? = null
)
