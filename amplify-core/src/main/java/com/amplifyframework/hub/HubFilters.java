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

import androidx.annotation.NonNull;

import com.amplifyframework.core.async.AmplifyOperation;

public final class HubFilters {

    private HubFilters() {
        // Since HubFilters is a utility class,
        // hiding the default constructor.
    }

    public static HubPayloadFilter always() {
        return new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                return true;
            }
        };
    }

    public static HubPayloadFilter all(@NonNull final HubPayloadFilter... filters) {
        return new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                boolean allFiltersSatisfied = true;
                for (HubPayloadFilter hubPayloadFilter: filters) {
                    allFiltersSatisfied &= hubPayloadFilter.filter(payload);
                }
                return allFiltersSatisfied;
            }
        };
    }

    public static HubPayloadFilter any(@NonNull final HubPayloadFilter... filters) {
        return new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                boolean anyFilterSatisfied = false;
                for (HubPayloadFilter hubPayloadFilter: filters) {
                    anyFilterSatisfied |= hubPayloadFilter.filter(payload);
                }
                return anyFilterSatisfied;
            }
        };
    }

    public static HubPayloadFilter and(@NonNull final HubPayloadFilter leftFilter,
                                       @NonNull final HubPayloadFilter rightFilter) {
        return new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                return leftFilter.filter(payload) && rightFilter.filter(payload);
            }
        };
    }

    public static HubPayloadFilter or(@NonNull final HubPayloadFilter leftFilter,
                                      @NonNull final HubPayloadFilter rightFilter) {
        return new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                return leftFilter.filter(payload) || rightFilter.filter(payload);
            }
        };
    }

    public static HubPayloadFilter hubPayloadFilter(@NonNull final AmplifyOperation<?> operation) {
        return new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull final HubPayload payload) {
                // TODO
                return true;
            }
        };
    }
}
