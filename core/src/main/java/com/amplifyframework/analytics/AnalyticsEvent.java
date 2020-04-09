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

import java.util.Objects;

/**
 * AnalyticsEvent is a custom analytics event that holds a name and a number of
 * {@link AnalyticsProperties}. This data object is used to indicate an event occurred such as a user taking
 * an action in your application.
 *
 * <pre>
 *     AnalyticsEvent event = AnalyticsEvent.builder()
 *          .name("LikedPost")
 *          .addProperty("PostType", "UserImage")
 *          .addProperty("LikedUserID", 78219)
 *          .addProperty("FirstLike", true)
 *          .build();
 * </pre>
 *
 * Once built, a AnalyticsEvent can be submitted to an analytics plugin through
 * {@link AnalyticsCategory#recordEvent(AnalyticsEventBehavior)}.
 */
public final class AnalyticsEvent implements AnalyticsEventBehavior {
    private final String name;
    private final AnalyticsProperties properties;

    private AnalyticsEvent(String name, AnalyticsProperties properties) {
        this.name = name;
        this.properties = properties;
    }

    /**
     * Returns the name of the event.
     *
     * @return The name of the event
     */
    @Override
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link AnalyticsProperties} of the event.
     *
     * @return The {@link AnalyticsProperties} of the event
     */
    @Override
    @NonNull
    public AnalyticsProperties getProperties() {
        return properties;
    }

    /**
     * Returns a new {@link Builder} to configure an instance of AnalyticsEvent.
     *
     * @return a {@link Builder}
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder is used to create and configure an instance of {@link AnalyticsEvent}. Its
     * methods return the Builder instance to allow for fluent method chaining. This Builder reuses
     * {@link AnalyticsProperties.Builder} to construct the properties to store in the event.
     *
     * @see AnalyticsProperties
     */
    public static final class Builder {
        private String name;
        private AnalyticsProperties.Builder propertiesBuilder;

        private Builder() {
            this.propertiesBuilder = AnalyticsProperties.builder();
        }

        /**
         * Adds a name to the {@link AnalyticsEvent} under construction.
         *
         * @param name The name of the event
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder name(@NonNull String name) {
            Objects.requireNonNull(name);

            this.name = name;
            return this;
        }

        /**
         * Adds a String property to the {@link AnalyticsEvent} under construction.
         *
         * @param name The name of the property
         * @param value The String value of the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder addProperty(@NonNull String name, @NonNull String value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);

            this.propertiesBuilder.add(name, value);
            return this;
        }

        /**
         * Adds a Double property to the {@link AnalyticsEvent} under construction.
         *
         * @param name The name of the property
         * @param value The Double value of the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder addProperty(@NonNull String name, @NonNull Double value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);

            this.propertiesBuilder.add(name, value);
            return this;
        }

        /**
         * Adds a Boolean property to the {@link AnalyticsEvent} under construction.
         *
         * @param name The name of the property
         * @param value The Boolean value of the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder addProperty(@NonNull String name, @NonNull Boolean value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);

            this.propertiesBuilder.add(name, value);
            return this;
        }

        /**
         * Adds an Integer property to the {@link AnalyticsEvent} under construction.
         *
         * @param name The name of the property
         * @param value The Integer value of the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder addProperty(@NonNull String name, @NonNull Integer value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);

            this.propertiesBuilder.add(name, value);
            return this;
        }
        
        /**
         * Returns the built {@link AnalyticsEvent}.
         *
         * @return The constructed {@link AnalyticsEvent} configured with the parameters set in
         * the Builder
         */
        @NonNull
        public AnalyticsEvent build() {
            return new AnalyticsEvent(name, propertiesBuilder.build());
        }
    }
}
