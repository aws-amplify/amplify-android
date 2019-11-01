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
 * A callback that received notifications of events surrounding async
 * operations. This can be used in cases where an event will be
 * generated.
 * @param <T> the parameter type of the event object.
 */
public interface EventListener<T> {

    /**
     * The event object will be passed through the
     * onEvent method.
     *
     * @param event the event object
     */
    void onEvent(T event);
}

