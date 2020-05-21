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
import com.amplifyframework.core.reachability.Reachability;
import com.amplifyframework.core.reachability.SocketHost;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposables;

/**
 * A utility to wait for AWS to become available.
 */
final class AwsReachability {
    private static final String AWS_WEBSITE_HOST_NAME = "aws.amazon.com";
    private static final int HTTPS_PORT = 443;

    private final Host host;
    private final Reachability reachability;

    AwsReachability(Reachability reachability) {
        this(
            SocketHost.from(AWS_WEBSITE_HOST_NAME, HTTPS_PORT),
            reachability
        );
    }

    AwsReachability(Host host, Reachability reachability) {
        this.host = host;
        this.reachability = reachability;
    }

    /**
     * Checks if AWS is reachable, right now.
     * @return True if AWS is reachable, false otherwise.
     */
    Single<Boolean> isReachable() {
        return Single.create(emitter -> emitter.onSuccess(reachability.isReachable(host)));
    }

    /**
     * Waits for AWS to become reachable.
     * @return A Completable which completes when AWS is reachable again
     */
    Completable awaitReachable() {
        return Completable.create(emitter -> {
            CompositeDisposable disposable = new CompositeDisposable();
            emitter.setDisposable(disposable);
            AtomicReference<Cancelable> cancelable = new AtomicReference<>();
            disposable.add(Disposables.fromRunnable(() -> {
                Cancelable current = cancelable.get();
                if (current != null) {
                    current.cancel();
                }
            }));
            cancelable.set(reachability.whenReachable(host, reachableHost -> emitter.onComplete()));
        });
    }
}
