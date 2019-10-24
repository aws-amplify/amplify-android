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
 * @param <T> Data type of the item or event being observed
 */
public interface Observer<T> {
    /**
     * Invoked upon successful subscription of this
     * object to an instance of {@link Observable}.
     * @param token identification token of the
     *              subscription
     */
    void onSubscribe(SubscriptionToken token);

    /**
     * Invoked upon successful reception of an emitted
     * event by an instance of {@link Observable} that
     * this object is subscribed to.
     * @param event emitted event or item
     */
    void onNext(T event);

    /**
     * Invoked upon error that terminates
     * subscription.
     *
     * Connection error such as authorization error
     * or network issues will invoke this method.
     * @param throwable termination cause
     */
    void onError(Throwable throwable);

    /**
     * Invoked upon successful termination of connection
     * to an observable without error.
     */
    void onComplete();
}
