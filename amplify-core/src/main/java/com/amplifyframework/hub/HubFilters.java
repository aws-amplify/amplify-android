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

import java.util.Objects;

/**
 * Provides some common implementations of the {@link HubPayloadFilter}.
 */
public final class HubFilters {

    private HubFilters() {
        throw new UnsupportedOperationException("No instances of the HubFilters utility, please.");
    }

    /**
     * Gets a {@link HubPayloadFilter} that always always matches the provided payload.
     * @return a filter that always returns true
     */
    @NonNull
    public static HubPayloadFilter always() {
        return payload -> true;
    }

    /**
     * Gets a {@link HubPayloadFilter} that returns true if and only if all of the
     * provides filters in the variable argument list return true.
     * @param filters A list of {@link HubPayloadFilter}; if all filters return
     *                true for a given payload, then the returned filter will do so,
     *                as well. If filters is an empty list, then the returned filter will
     *                return true. filters may contain null objects, they will be ignored.
     * @return A {@link HubPayloadFilter} which returns true only if all of the provided
     *         filters return true, as described
     */
    @NonNull
    public static HubPayloadFilter all(@NonNull final HubPayloadFilter... filters) {
        return payload -> {
            boolean allFiltersSatisfied = true;
            for (HubPayloadFilter hubPayloadFilter : filters) {
                if (hubPayloadFilter != null) {
                    allFiltersSatisfied &= hubPayloadFilter.filter(payload);
                }
            }
            return allFiltersSatisfied;
        };
    }

    /**
     * Gets a {@link HubPayloadFilter} instance that will return true if any of the
     * provided filters returns true.
     * @param filters A variable argument list of filters. If any filter returns true,
     *                then the composite filter that is returned will also return true.
     *                If the list is empty, then the composite filter will return false,
     *                because no filter (not "any") was a match. If a null filter is
     *                found in the list, it is ignored / not evaluated.
     * @return A composite {@link HubPayloadFilter} which will return true if and only if
     *         one or more of the provided filters returns true
     */
    @NonNull
    public static HubPayloadFilter any(@NonNull final HubPayloadFilter... filters) {
        return payload -> {
            boolean anyFilterSatisfied = false;
            for (HubPayloadFilter hubPayloadFilter: filters) {
                if (hubPayloadFilter != null) {
                    anyFilterSatisfied |= hubPayloadFilter.filter(payload);
                }
            }
            return anyFilterSatisfied;
        };
    }

    /**
     * Gets a composite {@link HubPayloadFilter} which will return true if both of
     * the provided filters return true. It is an error to provide a null argument.
     * @param leftFilter A non-null {@link HubPayloadFilter}
     * @param rightFilter A non-null {@link HubPayloadFilter}
     * @return A composite {@link HubPayloadFilter} that will return true if both
     *         of the provided filters return true
     */
    @NonNull
    public static HubPayloadFilter and(@NonNull final HubPayloadFilter leftFilter,
                                       @NonNull final HubPayloadFilter rightFilter) {
        Objects.requireNonNull(leftFilter);
        Objects.requireNonNull(rightFilter);
        return payload -> leftFilter.filter(payload) && rightFilter.filter(payload);
    }

    /**
     * Gets a composite {@link HubPayloadFilter} which will return true if either
     * of the provided filters return true. It is an error to provide a null argument.
     * @param leftFilter A non-null {@link HubPayloadFilter}
     * @param rightFilter A non-null {@link HubPayloadFilter}
     * @return A composite {@link HubPayloadFilter} which will return true if
     *         and only if one or both of the provided filters returns true
     */
    @NonNull
    public static HubPayloadFilter or(@NonNull final HubPayloadFilter leftFilter,
                                      @NonNull final HubPayloadFilter rightFilter) {
        Objects.requireNonNull(leftFilter);
        Objects.requireNonNull(rightFilter);
        return payload -> leftFilter.filter(payload) || rightFilter.filter(payload);
    }

    @SuppressWarnings("JavadocMethod") // Not clear how this will work until its implemented
    @NonNull
    public static HubPayloadFilter hubPayloadFilter(@NonNull final AmplifyOperation operation) {
        return payload -> /* TODO */ true;
    }
}

