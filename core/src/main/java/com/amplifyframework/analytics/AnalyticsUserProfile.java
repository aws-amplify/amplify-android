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

/**
 * Represents user specific data such as name, email, plan, location etc.
 */
public final class AnalyticsUserProfile {
    private String name;
    private String email;
    private String plan;
    private Location location;
    private Properties customProperties;

    public AnalyticsUserProfile(final String name,
                         final String email,
                         final String plan,
                         final Location location,
                         final Properties customProperties) {
        this.name = name;
        this.email = email;
        this.plan = plan;
        this.location = location;
        this.customProperties = customProperties;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPlan() {
        return plan;
    }

    public Location getLocation() {
        return location;
    }

    public Properties getCustomProperties() {
        return customProperties;
    }

    public static final class Location {
        private Double latitude;
        private Double longitude;
        private String postalCode;
        private String city;
        private String region;
        private String country;

        public Location(final Double latitude,
                 final Double longitude,
                 final String postalCode,
                 final String city,
                 final String region,
                 final String country) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.postalCode = postalCode;
            this.city = city;
            this.region = region;
            this.country = country;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public String getCity() {
            return city;
        }

        public String getRegion() {
            return region;
        }

        public String getCountry() {
            return country;
        }
    }
}
