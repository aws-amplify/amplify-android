/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.async;

/**
 * The event wraps the name, state, data of the event
 * and the operation that generated this event.
 */
public final class AsyncEvent<T> {

    public enum State {
        UNKNOWN,
        NOT_IN_PROCESS,
        IN_PROCESS,
        COMPLETED,
        FAILED;
    }

    private final String eventName;

    private final State eventState;

    private final T eventData;

    private final AmplifyOperation<?> generatedByAmplifyOperation;

    public AsyncEvent(String eventName,
                      State eventState,
                      T eventData,
                      AmplifyOperation<?> generatedByAmplifyOperation) {
        this.eventName = eventName;
        this.eventState = eventState;
        this.eventData = eventData;
        this.generatedByAmplifyOperation = generatedByAmplifyOperation;
    }

    public String getEventName() {
        return eventName;
    }

    public State getEventState() {
        return eventState;
    }

    public T getEventData() {
        return eventData;
    }

    public AmplifyOperation<?> getGeneratedByAmplifyOperation() {
        return generatedByAmplifyOperation;
    }
}
