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

package com.amplifyframework.core.stream;

/**
 * An observable emits a stream of items or events that
 * can be subscribed to by an observer.
 * @param <T> data type of the observable item or event
 */
public interface IObservable<T> {
    /**
     * Register an observer to be subscribed observable.
     * @param observer an instance of subscriber to
     *                 listen to this observable object
     */
    void subscribe(IObserver<T> observer);

    /**
     * Dispose of the subscription associated with given
     * token.
     * @param token subscription token to identify
     *              specific connection to terminate
     */
    void unsubscribe(SubscriptionToken token);
}

