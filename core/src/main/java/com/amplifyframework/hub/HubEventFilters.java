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

import java.util.Objects;

/**
 * Provides some common implementations of the {@link HubEventFilter}.
 */
public final class HubEventFilters {

    private HubEventFilters() {
        throw new UnsupportedOperationException("No instances of the HubEventFilters utility, please.");
    }

    /**
     * Gets a {@link HubEventFilter} that always always matches the provided event.
     * @return a filter that always returns true
     */
    @NonNull
    public static HubEventFilter always() {
        return event -> true;
    }

    /**
     * Gets a {@link HubEventFilter} that returns true if and only if all of the
     * provides filters in the variable argument list return true.
     * @param filters A list of {@link HubEventFilter}; if all filters return
     *                true for a given event, then the returned filter will do so,
     *                as well. If filters is an empty list, then the returned filter will
     *                return true. filters may contain null objects, they will be ignored.
     * @return A {@link HubEventFilter} which returns true only if all of the provided
     *         filters return true, as described
     */
    @NonNull
    public static HubEventFilter all(@NonNull final HubEventFilter... filters) {
        return event -> {
            boolean allFiltersSatisfied = true;
            for (HubEventFilter hubEventFilter : filters) {
                if (hubEventFilter != null) {
                    allFiltersSatisfied &= hubEventFilter.filter(event);
                }
            }
            return allFiltersSatisfied;
        };
    }

    /**
     * Gets a {@link HubEventFilter} instance that will return true if any of the
     * provided filters returns true.
     * @param filters A variable argument list of filters. If any filter returns true,
     *                then the composite filter that is returned will also return true.
     *                If the list is empty, then the composite filter will return false,
     *                because no filter (not "any") was a match. If a null filter is
     *                found in the list, it is ignored / not evaluated.
     * @return A composite {@link HubEventFilter} which will return true if and only if
     *         one or more of the provided filters returns true
     */
    @NonNull
    public static HubEventFilter any(@NonNull final HubEventFilter... filters) {
        return event -> {
            boolean anyFilterSatisfied = false;
            for (HubEventFilter hubEventFilter : filters) {
                if (hubEventFilter != null) {
                    anyFilterSatisfied |= hubEventFilter.filter(event);
                }
            }
            return anyFilterSatisfied;
        };
    }

    /**
     * Gets a composite {@link HubEventFilter} which will return true if both of
     * the provided filters return true. It is an error to provide a null argument.
     * @param leftFilter A non-null {@link HubEventFilter}
     * @param rightFilter A non-null {@link HubEventFilter}
     * @return A composite {@link HubEventFilter} that will return true if both
     *         of the provided filters return true
     */
    @NonNull
    public static HubEventFilter and(@NonNull final HubEventFilter leftFilter,
                                     @NonNull final HubEventFilter rightFilter) {
        Objects.requireNonNull(leftFilter);
        Objects.requireNonNull(rightFilter);
        return event -> leftFilter.filter(event) && rightFilter.filter(event);
    }

    /**
     * Gets a composite {@link HubEventFilter} which will return true if either
     * of the provided filters return true. It is an error to provide a null argument.
     * @param leftFilter A non-null {@link HubEventFilter}
     * @param rightFilter A non-null {@link HubEventFilter}
     * @return A composite {@link HubEventFilter} which will return true if
     *         and only if one or both of the provided filters returns true
     */
    @NonNull
    public static HubEventFilter or(@NonNull final HubEventFilter leftFilter,
                                    @NonNull final HubEventFilter rightFilter) {
        Objects.requireNonNull(leftFilter);
        Objects.requireNonNull(rightFilter);
        return event -> leftFilter.filter(event) || rightFilter.filter(event);
    }
}
