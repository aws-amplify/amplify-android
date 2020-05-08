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

package com.amplifyframework.predictions.models;

/**
 * Class to represent a pose by using three main
 * principal axes: pitch, roll, and yaw.
 *
 * @see <a href=https://en.wikipedia.org/wiki/Aircraft_principal_axes#Principal_axes>Principal axes</a>
 */
public final class Pose {
    private final double pitch;
    private final double roll;
    private final double yaw;

    /**
     * Constructs a new instance of {@link Pose} using three
     * principal axes of motion.
     * @param pitch the pitch
     * @param roll the roll
     * @param yaw the yaw
     */
    public Pose(double pitch, double roll, double yaw) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
    }

    /**
     * Gets the pitch value.
     * @return the pitch
     */
    public double getPitch() {
        return pitch;
    }

    /**
     * Gets the roll value.
     * @return the roll
     */
    public double getRoll() {
        return roll;
    }

    /**
     * Gets the yaw value.
     * @return the yaw
     */
    public double getYaw() {
        return yaw;
    }
}
