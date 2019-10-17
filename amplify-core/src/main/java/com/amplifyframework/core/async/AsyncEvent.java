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
 * @param <T> The type of data transmitted inside of this event
 */
public final class AsyncEvent<T> {

    /**
     * The state of a unit of asynchronous work.
     */
    public enum State {
        /**
         * The state of the work is not currently known.
         */
        UNKNOWN,

        /**
         * The asynchronous work unit is known not to be in progress,
         * but a final state (completed, failed) is not known at this time.
         */
        NOT_IN_PROCESS,

        /**
         * Asynchronous work is in progress.
         */
        IN_PROCESS,

        /**
         * Asynchronous work has completed with a successful outcome.
         */
        COMPLETED,

        /**
         * Asynchronous work is not in process, because it ended with
         * a failure state.
         */
        FAILED
    }

    private final String eventName;
    private final State eventState;
    private final T eventData;
    private final AmplifyOperation generatedByAmplifyOperation;

    /**
     * Construcst a new AsyncEvent.
     * @param eventName The name of the event
     * @param eventState The state of the event
     * @param eventData Data associated with the event
     * @param generatedByAmplifyOperation
     *        A reference to an operation which caused this event to be generated
     */
    public AsyncEvent(String eventName,
                      State eventState,
                      T eventData,
                      AmplifyOperation generatedByAmplifyOperation) {
        this.eventName = eventName;
        this.eventState = eventState;
        this.eventData = eventData;
        this.generatedByAmplifyOperation = generatedByAmplifyOperation;
    }

    /**
     * Gets the name of the event.
     * @return Event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Gets the state of the event.
     * @return Event state
     */
    public State getEventState() {
        return eventState;
    }

    /**
     * Gets data associated with the event.
     * @return Event data
     */
    public T getEventData() {
        return eventData;
    }

    /**
     * Gets a reference to an operation which generated this event.
     * @return A reference to an operation which generated this event
     */
    public AmplifyOperation getGeneratedByAmplifyOperation() {
        return generatedByAmplifyOperation;
    }
}

