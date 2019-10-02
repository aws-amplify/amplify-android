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

package com.amplifyframework.hub;

import android.support.annotation.NonNull;

import com.amplifyframework.core.async.AmplifyOperation;

public class HubFilters {
    public static HubFilter always() {
        return new HubFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                return true;
            }
        };
    }

    public static HubFilter all(@NonNull final HubFilter... filters) {
        return new HubFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                boolean allFiltersSatisfied = true;
                for (HubFilter hubFilter: filters) {
                    allFiltersSatisfied &= hubFilter.filter(payload);
                }
                return allFiltersSatisfied;
            }
        };
    }

    public static HubFilter any(@NonNull final HubFilter... filters) {
        return new HubFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                boolean anyFilterSatisfied = false;
                for (HubFilter hubFilter: filters) {
                    anyFilterSatisfied |= hubFilter.filter(payload);
                }
                return anyFilterSatisfied;
            }
        };
    }

    public static HubFilter and(@NonNull final HubFilter leftFilter, @NonNull final HubFilter rightFilter) {
        return new HubFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                return leftFilter.filter(payload) && rightFilter.filter(payload);
            }
        };
    }

    public static HubFilter or(@NonNull final HubFilter leftFilter, @NonNull final HubFilter rightFilter) {
        return new HubFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                return leftFilter.filter(payload) || rightFilter.filter(payload);
            }
        };
    }

    public static HubFilter hubFilter(@NonNull final AmplifyOperation operation) {
        return new HubFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                // TODO
                return true;
            }
        };
    }
}
