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

package com.amplifyframework.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * Represents user specific data such as name, email, plan, location etc.
 */
public class UserProfile {
    private final String name;
    private final String email;
    private final String plan;
    private final Location location;
    private final AnalyticsProperties customProperties;

    /**
     * Defines the only constructor for this class that should
     * be invoked by any classes inheriting from this one.
     * @param builder An instance of the builder with the desired properties set.
     */
    protected UserProfile(@NonNull Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.plan = builder.plan;
        this.location = builder.location;
        this.customProperties = builder.customProperties;
    }

    /**
     * Gets the user's name.
     * @return User's name
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Gets the user's email.
     * @return User's email
     */
    @Nullable
    public String getEmail() {
        return email;
    }

    /**
     * Gets the user's plan.
     * @return User's plan
     */
    @Nullable
    public String getPlan() {
        return plan;
    }

    /**
     * Gets the user's location.
     * @return User's location
     */
    @Nullable
    public Location getLocation() {
        return location;
    }

    /**
     * Gets any custom properties associated to the user.
     * @return User's custom properties
     */
    @Nullable
    public AnalyticsProperties getCustomProperties() {
        return customProperties;
    }

    /**
     * Begins construction of an {@link UserProfile} using a builder pattern.
     * @return An {@link UserProfile.Builder} instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * When extending this class, be sure to override this method to
     * include any other relevant fields.
     * @param object The object to compare this instance to.
     * @return True if they are equal, false otherwise.
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        UserProfile that = (UserProfile) object;

        if (!ObjectsCompat.equals(name, that.name)) {
            return false;
        }
        if (!ObjectsCompat.equals(email, that.email)) {
            return false;
        }
        if (!ObjectsCompat.equals(plan, that.plan)) {
            return false;
        }
        if (!ObjectsCompat.equals(location, that.location)) {
            return false;
        }
        return ObjectsCompat.equals(customProperties, that.customProperties);
    }

    /**
     * When extending this class, be sure to override this method and
     * include any relevant fields as part of the result.
     * @return The calculated hash code for the instance.
     */
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (plan != null ? plan.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (customProperties != null ? customProperties.hashCode() : 0);
        return result;
    }

    /**
     * When extending this class, be sure to override this method to
     * include any other relevant fields.
     * @return A string representation of the instance.
     */
    @NonNull
    @Override
    public String toString() {
        return "UserProfile{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", plan='" + plan + '\'' +
                ", location=" + location +
                ", customProperties=" + customProperties +
                '}';
    }

    /**
     * Builder for creating a UserProfile object.
     */
    public static class Builder {
        private String name;
        private String email;
        private String plan;
        private Location location;
        private AnalyticsProperties customProperties;

        /**
         * Configures the name to be used in the next-built UserProfile.
         * @param name User's name
         * @return Current builder instance, for method chaining
         */
        @NonNull
        public Builder name(@NonNull final String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        /**
         * Configures the email to be used in the next-built UserProfile.
         * @param email User's email
         * @return Current builder instance, for method chaining
         */
        @NonNull
        public Builder email(@NonNull final String email) {
            Objects.requireNonNull(email);
            this.email = email;
            return this;
        }

        /**
         * Configures the plan to be used in the next-built UserProfile.
         * @param plan User's plan
         * @return Current builder instance, for method chaining
         */
        @NonNull
        public Builder plan(@NonNull final String plan) {
            Objects.requireNonNull(plan);
            this.plan = plan;
            return this;
        }

        /**
         * Configures the location to be used in the next-built UserProfile.
         * @param location User's location
         * @return Current builder instance, for method chaining
         */
        @NonNull
        public Builder location(@NonNull final Location location) {
            Objects.requireNonNull(location);
            this.location = location;
            return this;
        }

        /**
         * Configures any additional, custom properties to be used in the next-built UserProfile.
         * @param properties Additional properties bound to the next-built user
         * @return Current builder instance, for method chaining
         */
        @NonNull
        public Builder customProperties(@NonNull final AnalyticsProperties properties) {
            Objects.requireNonNull(properties);
            this.customProperties = properties;
            return this;
        }

        /**
         * Builds an instance of {@link UserProfile}, using the provided values.
         * @return An {@link UserProfile}
         */
        @NonNull
        public UserProfile build() {
            return new UserProfile(this);
        }
    }

    /**
     * Represents a user's location.
     */
    public static final class Location {
        private final Double latitude;
        private final Double longitude;
        private final String postalCode;
        private final String city;
        private final String region;
        private final String country;

        private Location(@NonNull Builder builder) {
            this.latitude = builder.latitude;
            this.longitude = builder.longitude;
            this.postalCode = builder.postalCode;
            this.city = builder.city;
            this.region = builder.region;
            this.country = builder.country;
        }

        /**
         * Gets the user's last known latitude.
         * @return User's latitude
         */
        @Nullable
        public Double getLatitude() {
            return latitude;
        }

        /**
         * Gets the user's last known longitude.
         * @return User's longitude
         */
        @Nullable
        public Double getLongitude() {
            return longitude;
        }

        /**
         * Gets the user's postal code.
         * @return User's postal code
         */
        @Nullable
        public String getPostalCode() {
            return postalCode;
        }

        /**
         * Gets the user's city.
         * @return User's city
         */
        @Nullable
        public String getCity() {
            return city;
        }

        /**
         * Gets the user's region.
         * @return User's region
         */
        @Nullable
        public String getRegion() {
            return region;
        }

        /**
         * Gets the user's country.
         * @return User's country
         */
        @Nullable
        public String getCountry() {
            return country;
        }

        /**
         * Builds a {@link Location}.
         * @return A new {@link Location}
         */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }

            Location location = (Location) object;

            if (!ObjectsCompat.equals(latitude, location.latitude)) {
                return false;
            }
            if (!ObjectsCompat.equals(longitude, location.longitude)) {
                return false;
            }
            if (!ObjectsCompat.equals(postalCode, location.postalCode)) {
                return false;
            }
            if (!ObjectsCompat.equals(city, location.city)) {
                return false;
            }
            if (!ObjectsCompat.equals(region, location.region)) {
                return false;
            }
            return ObjectsCompat.equals(country, location.country);
        }

        @Override
        public int hashCode() {
            int result = latitude != null ? latitude.hashCode() : 0;
            result = 31 * result + (longitude != null ? longitude.hashCode() : 0);
            result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
            result = 31 * result + (city != null ? city.hashCode() : 0);
            result = 31 * result + (region != null ? region.hashCode() : 0);
            result = 31 * result + (country != null ? country.hashCode() : 0);
            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return "Location{" +
                    "latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", postalCode='" + postalCode + '\'' +
                    ", city='" + city + '\'' +
                    ", region='" + region + '\'' +
                    ", country='" + country + '\'' +
                    '}';
        }

        /**
         * Builder for creating a Location object.
         */
        public static final class Builder {
            private Double latitude;
            private Double longitude;
            private String postalCode;
            private String city;
            private String region;
            private String country;

            /**
             * Configures the latitude to use in the next-build {@link Location}.
             * @param latitude user's latitude
             * @return Current builder instance, for method chaining
             */
            @NonNull
            public Builder latitude(@NonNull Double latitude) {
                Objects.requireNonNull(latitude);
                this.latitude = latitude;
                return this;
            }

            /**
             * Configures the longitude to use in the next-build {@link Location}.
             * @param longitude user's longitude
             * @return Current builder instance, for method chaining
             */
            @NonNull
            public Builder longitude(@NonNull Double longitude) {
                Objects.requireNonNull(longitude);
                this.longitude = longitude;
                return this;
            }

            /**
             * Configures the postal code to use in the next-build {@link Location}.
             * @param postalCode user's postal code
             * @return Current builder instance, for method chaining
             */
            @NonNull
            public Builder postalCode(@NonNull String postalCode) {
                Objects.requireNonNull(postalCode);
                this.postalCode = postalCode;
                return this;
            }

            /**
             * Configures the city to use in the next-build {@link Location}.
             * @param city user's city
             * @return Current builder instance, for method chaining
             */
            @NonNull
            public Builder city(@NonNull String city) {
                Objects.requireNonNull(city);
                this.city = city;
                return this;
            }

            /**
             * Configures the region to use in the next-build {@link Location}.
             * @param region user's region
             * @return Current builder instance, for method chaining
             */
            @NonNull
            public Builder region(@NonNull String region) {
                Objects.requireNonNull(region);
                this.region = region;
                return this;
            }

            /**
             * Configures the country to use in the next-build {@link Location}.
             * @param country user's country
             * @return Current builder instance, for method chaining
             */
            @NonNull
            public Builder country(@NonNull String country) {
                Objects.requireNonNull(country);
                this.country = country;
                return this;
            }

            /**
             * Builds a {@link Location}.
             * @return A {@link Location}
             */
            @NonNull
            public Location build() {
                return new Location(this);
            }
        }
    }
}
