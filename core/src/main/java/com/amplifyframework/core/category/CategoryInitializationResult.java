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

package com.amplifyframework.core.category;

import androidx.annotation.NonNull;

import com.amplifyframework.core.InitializationResult;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A result of a category initialization.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class CategoryInitializationResult {
    private final InitializationStatus initializationStatus;
    private final Map<String, InitializationResult> pluginInitializationResults;

    private CategoryInitializationResult(
            @NonNull InitializationStatus initializationStatus,
            @NonNull Map<String, InitializationResult> pluginInitializationResults) {
        this.initializationStatus = initializationStatus;
        this.pluginInitializationResults = pluginInitializationResults;
    }

    /**
     * Creates a CategoryInitializationResult using the results from initializing a collection
     * of named plugins.
     * @param pluginResults The results of initialization for a collection of plugins
     * @return A category initialization result
     */
    @NonNull
    public static CategoryInitializationResult with(@NonNull Map<String, InitializationResult> pluginResults) {
        Objects.requireNonNull(pluginResults);
        InitializationStatus categoryStatus;
        if (anyFailed(pluginResults)) {
            categoryStatus = InitializationStatus.FAILED;
        } else {
            categoryStatus = InitializationStatus.SUCCEEDED;
        }
        return new CategoryInitializationResult(categoryStatus, pluginResults);
    }

    private static boolean anyFailed(@NonNull Map<String, InitializationResult> pluginResults) {
        for (InitializationResult result : pluginResults.values()) {
            if (result.isFailure()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the initialization status of the category, e.g. {@link InitializationStatus#SUCCEEDED}.
     * @return The category's initialization status
     */
    @NonNull
    public InitializationStatus getInitializationStatus() {
        return initializationStatus;
    }

    /**
     * Checks if the initialization is a success.
     * @return True if the category was initialized successfully.
     */
    public boolean isSuccess() {
        return InitializationStatus.SUCCEEDED.equals(initializationStatus);
    }

    /**
     * Checks if the category initialization failed to to a/an error(s).
     * @return True if category initialization failed
     */
    public boolean isFailure() {
        return InitializationStatus.FAILED.equals(initializationStatus);
    }


    /**
     * Gets the failures associated with failed plugins.
     * @return A map of plugin key to thrown exception which caused the failure
     */
    @NonNull
    public Map<String, Throwable> getPluginInitializationFailures() {
        Map<String, Throwable> failureByPluginKey = new HashMap<>();
        for (Map.Entry<String, InitializationResult> initializationResult : pluginInitializationResults.entrySet()) {
            Throwable failure = initializationResult.getValue().getFailure();
            String pluginKey = initializationResult.getKey();
            if (failure != null) {
                failureByPluginKey.put(pluginKey, failure);
            }
        }
        return Immutable.of(failureByPluginKey);
    }

    /**
     * Gets the results for any failed plugins.
     * @return A map of plugin names to plugin initialization results, only for those that failed.
     */
    @NonNull
    public Set<String> getFailedPlugins() {
        return filterPluginResults(entry -> entry.getValue().isFailure());
    }

    /**
     * Gets the results for all successful plugins.
     * @return A map of plugin names to plugin initialization results, only for those that succeeded.
     */
    @NonNull
    public Set<String> getSuccessfulPlugins() {
        return filterPluginResults(entry -> entry.getValue().isSuccess());
    }

    private Set<String> filterPluginResults(
            @NonNull PluginCriteria<Map.Entry<String, InitializationResult>> pluginCriteria) {
        Set<String> results = new HashSet<>();
        for (Map.Entry<String, InitializationResult> pluginResult : pluginInitializationResults.entrySet()) {
            if (pluginCriteria.appliesTo(pluginResult)) {
                results.add(pluginResult.getKey());
            }
        }
        return results;
    }

    interface PluginCriteria<T> {
        boolean appliesTo(T item);
    }
}
