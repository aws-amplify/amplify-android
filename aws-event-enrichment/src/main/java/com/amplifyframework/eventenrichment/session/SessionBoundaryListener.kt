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
package com.amplifyframework.eventenrichment.session

/** A session start or stop, with the event type it is reported as. */
internal enum class SessionBoundary(val eventType: String) {
    START("_session.start"),
    STOP("_session.stop")
}

/**
 * Receives session boundaries from [SessionManager].
 *
 * Invoked while the session lock is held, in transition order, so
 * implementations must return promptly and must not call back into the manager.
 */
internal fun interface SessionBoundaryListener {
    /**
     * Called when [session] starts or stops. A stopped [session] carries its
     * stop timestamp and duration.
     */
    fun onBoundary(boundary: SessionBoundary, session: Session)
}
