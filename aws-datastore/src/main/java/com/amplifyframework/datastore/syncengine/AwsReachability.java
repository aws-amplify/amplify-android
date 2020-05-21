/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.reachability.Host;
import com.amplifyframework.core.reachability.PeriodicReachabilityChecker;
import com.amplifyframework.core.reachability.Reachability;
import com.amplifyframework.core.reachability.SocketHost;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;

/**
 * A utility to wait for AWS to become available.
 */
public final class AwsReachability {
    private final Host host;
    private final Reachability reachability;

    AwsReachability(Host host) {
        this.host = host;
        this.reachability = PeriodicReachabilityChecker.instance();
    }

    /**
     * Waits for AWS to become reachable.
     * @return A Completable which completes when AWS is reachable again
     */
    Completable isReachable() {
        return Completable.create(emitter -> {
            CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            Cancelable cancelable =
                reachability.whenReachable(host, reachableHost -> emitter.onComplete());
            disposable.add(Disposables.fromAction(cancelable::cancel));
        });
    }
}
