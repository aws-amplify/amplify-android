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

import androidx.annotation.NonNull;

import com.amplifyframework.geo.models.GeoPosition;

import java.util.Date;
import java.util.List;

public class GeoTrackingSessionOptions {
    /**
     * The amount of power required.
     */
    Power powerRequired;

    /**
     * The desired accuracy for location updates.
     */
    Accuracy desiredAccuracy;

    /**
     * By default, location updates are persisted if the service cannot be reached
     * due to loss of network connectivity, and the updates are sent once an update
     * is possible. Setting this value to `true` will disable this behavior.
     */
    boolean disregardLocationUpdatesWhenOffline;

    /**
     * Name of tracker resource. Set to the default tracker if no tracker is passed in.
     */
    String tracker;

    /**
     * The minimum update distance between location updates in meters. Valid values
     * are 0...Float.MAX_VALUE. If a potential location update doesn't cross the
     * minimum update distance threshold from the previous location update, it will
     * not occur. 0 represents no minimum update distance. By default, there is no
     * minimum distance between location updates.
     */
    float minUpdateDistanceMeters;

    /**
     * Minimum time in milliseconds between location updates. New location updates
     * will only occur if this interval has expired since the previous location
     * update. By default, there is no minimum time between location updates.
     */
    long minUpdatesInterval;

    /**
     * The maximum amount of location updates this Location Manager will receive
     * before stopping requests. Valid values are 1...Integer.MAX_VALUE. Default
     * value is Integer.MAX_VALUE.
     */
    int maxUpdates;

    /**
     * The date and time after which to stop tracking. By default, tracking will
     * continue until stopTracking(...) is called.
     */
    Date trackUntil;

    /**
     * Options for sending location updates in batches. Default is no batching.
     */
    BatchingOptions batchingOptions;
    public BatchingOptions getBatchingOptions() {
        return batchingOptions;
    }

    /**
     * Receives location updates. Default implementation is to save location
     * updates to Amazon Location service.
     */
    LocationProxyDelegate proxyDelegate;
    public LocationProxyDelegate getProxyDelegate() {
        return proxyDelegate;
    }

    public interface LocationProxyDelegate {
        void updatePositions(List<GeoPosition> positions);
    }

    /**
     * Returns a new builder instance for constructing {@link GeoTrackingSessionOptions}.
     *
     * @return a new builder instance.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static GeoTrackingSessionOptions defaults() {
        return new Builder().build();
    }

    public long getMinUpdatesInterval() {
        return minUpdatesInterval;
    }

    public float getMinUpdateDistanceMeters() {
        return minUpdateDistanceMeters;
    }

    public Power getPowerRequired() {
        return powerRequired;
    }

    public Accuracy getDesiredAccuracy() {
        return desiredAccuracy;
    }

    public boolean getDisregardLocationUpdatesWhenOffline() {
        return disregardLocationUpdatesWhenOffline;
    }

    public int getMaxUpdates() {
        return maxUpdates;
    }

    public Date getTrackUntil() {
        return trackUntil;
    }

    public String getTracker() {
        return tracker;
    }
    public void setTracker(String tracker) {
        this.tracker = tracker;
    }

    private GeoTrackingSessionOptions(Builder b) {
        this.powerRequired = b.powerRequired;
        this.desiredAccuracy = b.desiredAccuracy;
        this.disregardLocationUpdatesWhenOffline = b.disregardLocationUpdatesWhenOffline;
        this.tracker = b.tracker;
        this.minUpdateDistanceMeters = b.minUpdateDistanceMeters;
        this.minUpdatesInterval = b.minUpdatesInterval;
        this.maxUpdates = b.maxUpdates;
        this.trackUntil = b.trackUntil;
        this.batchingOptions = b.batchingOptions;
        this.proxyDelegate = b.proxyDelegate;
    }

    public static class Builder {
        /**
         * The amount of power required.
         */
        Power powerRequired = Power.LOW;

        /**
         * The desired accuracy for location updates.
         */
        Accuracy desiredAccuracy = Accuracy.COARSE;

        /**
         * By default, location updates are persisted if the service cannot be reached
         * due to loss of network connectivity, and the updates are sent once an update
         * is possible. Setting this value to `true` will disable this behavior.
         */
        boolean disregardLocationUpdatesWhenOffline = false;

        /**
         * Name of tracker resource. Set to the default tracker if no tracker is passed in.
         */
        String tracker = null;

        /**
         * The minimum update distance between location updates in meters. Valid values
         * are 0...Float.MAX_VALUE. If a potential location update doesn't cross the
         * minimum update distance threshold from the previous location update, it will
         * not occur. 0 represents no minimum update distance. By default, there is no
         * minimum distance between location updates.
         */
        float minUpdateDistanceMeters = 0;

        /**
         * Minimum time in milliseconds between location updates. New location updates
         * will only occur if this interval has expired since the previous location
         * update. By default, there is no minimum time between location updates.
         */
        long minUpdatesInterval = 0;

        /**
         * The maximum amount of location updates this Location Manager will receive
         * before stopping requests. Valid values are 1...Integer.MAX_VALUE. Default
         * value is Integer.MAX_VALUE.
         */
        int maxUpdates = Integer.MAX_VALUE;

        /**
         * The date and time after which to stop tracking. By default, tracking will
         * continue until stopTracking(...) is called.
         */
        Date trackUntil = null;

        /**
         * Options for sending location updates in batches. Default is no batching.
         */
        BatchingOptions batchingOptions = BatchingOptions.none();

        /**
         * Receives location updates. Default implementation is to save location
         * updates to Amazon Location service.
         */
        LocationProxyDelegate proxyDelegate = null;

        public Builder withPowerRequired(Power powerRequired) {
            this.powerRequired = powerRequired;
            return this;
        }

        public Builder withDesiredAccuracy(Accuracy desiredAccuracy) {
            this.desiredAccuracy = desiredAccuracy;
            return this;
        }

        public Builder withDisregardLocationUpdatesWhenOffline(boolean disregardLocationUpdatesWhenOffline) {
            this.disregardLocationUpdatesWhenOffline = disregardLocationUpdatesWhenOffline;
            return this;
        }

        public Builder withTracker(String tracker) {
            this.tracker = tracker;
            return this;
        }

        public Builder withMinUpdateDistanceMeters(float minUpdateDistanceMeters) {
            this.minUpdateDistanceMeters = minUpdateDistanceMeters;
            return this;
        }

        public Builder withMinUpdatesInterval(long minUpdatesInterval) {
            this.minUpdatesInterval = minUpdatesInterval;
            return this;
        }

        public Builder withMaxUpdates(int maxUpdates) {
            this.maxUpdates = maxUpdates;
            return this;
        }

        public Builder withTrackUntil(Date trackUntil) {
            this.trackUntil = trackUntil;
            return this;
        }

        public Builder withBatchingOptions(BatchingOptions batchingOptions) {
            this.batchingOptions = batchingOptions;
            return this;
        }

        public Builder withProxyDelegate(LocationProxyDelegate proxyDelegate) {
            this.proxyDelegate = proxyDelegate;
            return this;
        }

        public GeoTrackingSessionOptions build() {
            return new GeoTrackingSessionOptions(this);
        }
    }

    public enum Power {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum Accuracy {
        COARSE, // approximate location
        FINE // precise location
    }
}
