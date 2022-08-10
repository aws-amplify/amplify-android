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

package com.amplifyframework.statemachine

import java.util.Date
import java.util.UUID

interface StateMachineEvent {

    /**
     * Unique event identifier
     */
    val id: String
        get() = UUID.randomUUID().toString()

    /**
     * Describe the type of event related to the originating occurrence.
     */
    val type: String

    /**
     * Timestamp of when the occurrence happened.
     */
    val time: Date?
        get() = Date()
}
