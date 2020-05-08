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

import androidx.annotation.NonNull;

import com.fasterxml.uuid.Generators;

import java.util.Objects;
import java.util.UUID;

/**
 * A version 1, time-based UUID.
 *
 * This is uses instead of {@link UUID} directly, so that we can encapsulate
 * how the UUID is generated. This implementation uses FasterXML's Java UUID Generator.
 * But, we want to encapsulate that behind our own facade, here.
 *
 * The class is {@link Comparable}, so that we can sort based on timestamp.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc4122.txt">RFC 4122</a>
 */
final class TimeBasedUuid implements Comparable<TimeBasedUuid> {
    private final UUID delegate;

    private TimeBasedUuid(UUID delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a time-based UUID.
     * @return A time-based UUID
     */
    static TimeBasedUuid create() {
        UUID delegate = Generators.timeBasedGenerator().generate();
        validateVersion(delegate);
        return new TimeBasedUuid(delegate);
    }

    static TimeBasedUuid fromString(@NonNull String uuid) {
        Objects.requireNonNull(uuid);
        UUID delegate = UUID.fromString(uuid);
        validateVersion(delegate);
        return new TimeBasedUuid(delegate);
    }

    private static void validateVersion(UUID delegate) {
        if (1 != delegate.version()) {
            throw new IllegalStateException("Found UUID that is not a V1, time-based, UUID.");
        }
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        TimeBasedUuid that = (TimeBasedUuid) thatObject;

        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public int compareTo(@NonNull TimeBasedUuid another) {
        Objects.requireNonNull(another);
        return (int) Math.signum(this.delegate.timestamp() - another.delegate.timestamp());
    }
}
