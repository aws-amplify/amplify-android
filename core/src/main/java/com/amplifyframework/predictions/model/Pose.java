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

package com.amplifyframework.predictions.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Class to represent a pose by using three main
 * positions: pitch, roll, and yaw.
 */
public final class Pose {
    private final Double pitch;
    private final Double roll;
    private final Double yaw;

    private Pose(
            @NonNull Double pitch,
            @NonNull Double roll,
            @NonNull Double yaw
    ) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
    }

    /**
     * Gets the pitch value.
     * @return the pitch
     */
    @NonNull
    public Double getPitch() {
        return pitch;
    }

    /**
     * Gets the roll value.
     * @return the roll
     */
    @NonNull
    public Double getRoll() {
        return roll;
    }

    /**
     * Gets the yaw value.
     * @return the yaw
     */
    @NonNull
    public Double getYaw() {
        return yaw;
    }

    /**
     * Construct an immutable instance of {@link Pose} from
     * the three positional values: pitch, roll, and yaw.
     * @param pitch the pitch
     * @param roll the roll
     * @param yaw the yaw
     * @return An instance of {@link Pose}
     */
    @NonNull
    public static Pose from(
            @NonNull Double pitch,
            @NonNull Double roll,
            @NonNull Double yaw
    ) {
        return new Pose(
                Objects.requireNonNull(pitch),
                Objects.requireNonNull(roll),
                Objects.requireNonNull(yaw)
        );
    }
}
