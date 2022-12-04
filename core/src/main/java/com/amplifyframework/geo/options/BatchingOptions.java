/*
 *
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 *
 *
 */

package com.amplifyframework.geo.options;

import com.amplifyframework.geo.models.GeoPosition;

import java.util.OptionalInt;

public class BatchingOptions {
    /**
     * Number of seconds elapsed since sending location update(s) (to Amazon
     * Location Service for example) before location updates are sent again.
     */
    private OptionalInt secondsElapsed = OptionalInt.empty();

    public OptionalInt getSecondsElapsed() {
        return secondsElapsed;
    }

    /**
     * Send batch of location updates (to Amazon Location Service for example)
     * after at least this distance has been travelled. The computation of
     * distance travelled can be defined in one of two ways: 1) straight-line
     * distance between the last saved location and the most recent location in
     * the batch; or 2) cumulative distance between each location in the batch.
     * Which option to choose is up for discussion.
     */
    private OptionalInt distanceTravelled = OptionalInt.empty();

    public OptionalInt getDistanceTravelled() {
        return distanceTravelled;
    }

    // would this position exceed the batch?
    public boolean thresholdReached(GeoPosition first, GeoPosition last) {
        if (secondsElapsed.isPresent() &&
                secondsElapsed.getAsInt() < last.timeStamp.getTime() - first.timeStamp.getTime()) {
            return true;
        }
        // TODO: distance batching
//        if (distanceTravelled.isPresent() &&
//                distanceTravelled.getAsInt() < last.location.)
        return false;
    }

    private BatchingOptions() {}

    private void setSecondsElapsed(int secondsElapsed) {
        this.secondsElapsed = OptionalInt.of(secondsElapsed);
    }

    private void setDistanceTravelled(int distanceTravelled) {
        this.distanceTravelled = OptionalInt.of(distanceTravelled);
    }

    public static BatchingOptions none() {
        return new BatchingOptions();
    }

    public static BatchingOptions secondsElapsed(int secondsElapsed) {
        BatchingOptions options = new BatchingOptions();
        options.setSecondsElapsed(secondsElapsed);
        return options;
    }

    public static BatchingOptions distanceTravelled(int distanceTravelled) {
        BatchingOptions options = new BatchingOptions();
        options.setDistanceTravelled(distanceTravelled);
        return options;
    }
}
