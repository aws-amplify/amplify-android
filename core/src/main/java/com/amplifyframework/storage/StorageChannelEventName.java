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

package com.amplifyframework.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

import java.util.Objects;

/**
 * An enumeration of the names of events relating the the {@link StorageCategory},
 * that are published via {@link HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#DATASTORE} channel.
 */
public enum StorageChannelEventName {

    /**
     * An error occurred while uploading a file.
     */
    UPLOAD_ERROR("upload_error"),

    /**
     * The state of an upload has changed.
     */
    UPLOAD_STATE("upload_state"),

    /**
     * Progress has been made on an upload.
     */
    UPLOAD_PROGRESS("upload_progress"),

    /**
     * A download has erred out.
     */
    DOWNLOAD_ERROR("download_error"),

    /**
     * A download has undergone a state change.
     */
    DOWNLOAD_STATE("download_state"),

    /**
     * A download has made some progress.
     */
    DOWNLOAD_PROGRESS("download_progress");

    private final String hubEventName;

    /**
     * Enumerate the name of an even that is published on Hub, on the {@link HubChannel#STORAGE} channel.
     * @param hubEventName The name of an event to use when creating an {@link HubEvent}
     */
    StorageChannelEventName(@NonNull String hubEventName) {
        this.hubEventName = Objects.requireNonNull(hubEventName);
    }

    @NonNull
    @Override
    public String toString() {
        return hubEventName;
    }

    /**
     * Check if the provided string is one of the enumerated hub event names used by the
     * {@link StorageCategory}.
     * @param possiblyMatchingEventName Possibly, the name of a Hub event published by Storage
     * @return The enumerated event name, if found
     * @throws IllegalArgumentException If the provided string is not a known event name
     */
    @NonNull
    public static StorageChannelEventName fromString(@Nullable String possiblyMatchingEventName) {
        for (StorageChannelEventName possibleMatch : StorageChannelEventName.values()) {
            if (possibleMatch.toString().equals(possiblyMatchingEventName)) {
                return possibleMatch;
            }
        }
        final String errorMessage =
            "Storage category does not publish any Hub event with name = " + possiblyMatchingEventName;
        throw new IllegalArgumentException(errorMessage);
    }
}
