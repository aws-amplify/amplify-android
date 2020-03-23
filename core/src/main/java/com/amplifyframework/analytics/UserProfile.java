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

import java.util.Objects;

/**
 * Represents user specific data such as name, email, plan, location etc.
 */
public final class UserProfile {
    private final String name;
    private final String email;
    private final String plan;
    private final Location location;
    private final Properties customProperties;

    private UserProfile(@NonNull Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.plan = builder.plan;
        this.location = builder.location;
        this.customProperties = builder.customProperties;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getPlan() {
        return plan;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    @Nullable
    public Properties getCustomProperties() {
        return customProperties;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("checkstyle:NeedBraces")
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        UserProfile that = (UserProfile) object;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        if (plan != null ? !plan.equals(that.plan) : that.plan != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null)
            return false;
        return customProperties != null ? customProperties.equals(that.customProperties) :
                that.customProperties == null;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (plan != null ? plan.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (customProperties != null ? customProperties.hashCode() : 0);
        return result;
    }

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

    public static final class Builder {
        private String name;
        private String email;
        private String plan;
        private Location location;
        private Properties customProperties;

        @NonNull
        public Builder name(@NonNull final String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        @NonNull
        public Builder email(@NonNull final String email) {
            Objects.requireNonNull(email);
            this.email = email;
            return this;
        }

        @NonNull
        public Builder plan(@NonNull final String plan) {
            Objects.requireNonNull(plan);
            this.plan = plan;
            return this;
        }

        @NonNull
        public Builder location(@NonNull final Location location) {
            Objects.requireNonNull(location);
            this.location = location;
            return this;
        }

        @NonNull
        public Builder customProperties(@NonNull final Properties properties) {
            Objects.requireNonNull(properties);
            this.customProperties = properties;
            return this;
        }

        @NonNull
        public UserProfile build() {
            return new UserProfile(this);
        }
    }

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

        @Nullable
        public Double getLatitude() {
            return latitude;
        }

        @Nullable
        public Double getLongitude() {
            return longitude;
        }

        @Nullable
        public String getPostalCode() {
            return postalCode;
        }

        @Nullable
        public String getCity() {
            return city;
        }

        @Nullable
        public String getRegion() {
            return region;
        }

        @Nullable
        public String getCountry() {
            return country;
        }

        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        @SuppressWarnings("checkstyle:NeedBraces")
        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            Location location = (Location) object;

            if (latitude != null ? !latitude.equals(location.latitude) : location.latitude != null)
                return false;
            if (longitude != null ? !longitude.equals(location.longitude) : location.longitude != null)
                return false;
            if (postalCode != null ? !postalCode.equals(location.postalCode) : location.postalCode != null)
                return false;
            if (city != null ? !city.equals(location.city) : location.city != null) return false;
            if (region != null ? !region.equals(location.region) : location.region != null)
                return false;
            return country != null ? country.equals(location.country) : location.country == null;
        }

        @SuppressWarnings("checkstyle:MagicNumber")
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

        public static final class Builder {
            private Double latitude;
            private Double longitude;
            private String postalCode;
            private String city;
            private String region;
            private String country;

            @NonNull
            public Builder latitude(@NonNull Double latitude) {
                Objects.requireNonNull(latitude);
                this.latitude = latitude;
                return this;
            }

            @NonNull
            public Builder longitude(@NonNull Double longitude) {
                Objects.requireNonNull(longitude);
                this.longitude = longitude;
                return this;
            }

            @NonNull
            public Builder postalCode(@NonNull String postalCode) {
                Objects.requireNonNull(postalCode);
                this.postalCode = postalCode;
                return this;
            }

            @NonNull
            public Builder city(@NonNull String city) {
                Objects.requireNonNull(city);
                this.city = city;
                return this;
            }

            @NonNull
            public Builder region(@NonNull String region) {
                Objects.requireNonNull(region);
                this.region = region;
                return this;
            }

            @NonNull
            public Builder country(@NonNull String country) {
                Objects.requireNonNull(country);
                this.country = country;
                return this;
            }

            @NonNull
            public Location build() {
                return new Location(this);
            }
        }
    }
}
