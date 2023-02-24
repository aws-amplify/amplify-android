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

package com.amplifyframework.notifications.pushnotifications

import com.amplifyframework.notifications.NotificationsException

/**
 * Exception thrown by Push Notifications category plugins.
 * Creates a new exception with a message, root cause, and recovery suggestion.
 * @param message An error message describing why this exception was thrown
 * @param cause The underlying cause of this exception
 * @param recoverySuggestion Text suggesting a way to recover from the error being described
 */
open class PushNotificationsException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : NotificationsException(message, recoverySuggestion, cause)
