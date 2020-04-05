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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * AnalyticsProperties provides key/value pairs to represent attributes of an {@link AnalyticsEventBehavior}. While
 * different analytics services will have support for different datatypes, AnalyticsProperties provides a
 * baseline of support for String, Boolean, Double, and Integer. If one of these aren't available in
 * a given service, the plugin is expected to cast it to something supported. (e.g., convert Boolean
 * true to the String "true")
 *
 * <pre>
 *     AnalyticsProperties properties = AnalyticsProperties.builder()
 *          .add("PostType", "UserImage")
 *          .add("LikedUserID", 78219)
 *          .add("FirstLike", true)
 *          .build();
 * </pre>
 */
public final class AnalyticsProperties implements Iterable<Map.Entry<String, AnalyticsPropertyBehavior<?>>> {
    private final Map<String, AnalyticsPropertyBehavior<?>> properties;

    private AnalyticsProperties(Map<String, AnalyticsPropertyBehavior<?>> properties) {
        this.properties = properties;
    }

    /**
     * Iterator allows AnalyticsProperties to support foreach operations.
     *
     * <pre>
     *      for (Map.Entry&lt;String, AnalyticsPropertyBehavior&lt;?&gt;&gt; entry : properties) {
     *          String key = entry.getKey();
     *          AnalyticsPropertyBehavior&lt;?&gt; value = entry.getValue();
     *
     *          // Do something with key and value
     *      }
     * </pre>
     *
     * @return An {@link Iterator} of the underlying {@link Map#entrySet()} to enable foreach access
     */
    @Override
    @NonNull
    public Iterator<Map.Entry<String, AnalyticsPropertyBehavior<?>>> iterator() {
        return properties.entrySet().iterator();
    }

    /**
     * size returns the number of properties.
     *
     * @return The number of properties
     */
    @NonNull
    public int size() {
        return properties.size();
    }

    /**
     * get returns a specific {@link AnalyticsPropertyBehavior} by its name.
     *
     * @param propertyName The name of the property to retrieve
     * @throws NoSuchElementException If no property can be found by the given propertyName
     * @return The corresponding {@link AnalyticsPropertyBehavior}
     */
    @NonNull
    public AnalyticsPropertyBehavior<?> get(@NonNull String propertyName) throws NoSuchElementException {
        AnalyticsPropertyBehavior<?> property = properties.get(propertyName);

        if (property == null) {
            throw new NoSuchElementException("AnalyticsPropertyBehavior not found " + propertyName);
        }

        return properties.get(propertyName);
    }

    /**
     * Returns a new {@link Builder} to configure an instance of AnalyticsProperties.
     *
     * @return a {@link Builder}
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder is used to create and configure an instance of {@link AnalyticsProperties}. Its
     * methods return the Builder instance to allow for fluent method chaining.
     */
    public static final class Builder {
        private final Map<String, AnalyticsPropertyBehavior<?>> properties;

        /**
         * Constructor that returns a new Builder.
         */
        public Builder() {
            this.properties = new HashMap<>();
        }

        /**
         * Adds a {@link AnalyticsStringProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key A name for the property
         * @param value A String to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull String key, @NonNull String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.properties.put(key, AnalyticsStringProperty.from(value));
            return this;
        }

        /**
         * Adds a {@link AnalyticsDoubleProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key A name for the property
         * @param value A Double to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull String key, @NonNull Double value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.properties.put(key, AnalyticsDoubleProperty.from(value));
            return this;
        }

        /**
         * Adds a {@link AnalyticsBooleanProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key A name for the property
         * @param value A Boolean to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull String key, @NonNull Boolean value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.properties.put(key, AnalyticsBooleanProperty.from(value));
            return this;
        }

        /**
         * Adds an {@link AnalyticsIntegerProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key A name for the property
         * @param value An Integer to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull String key, @NonNull Integer value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.properties.put(key, AnalyticsIntegerProperty.from(value));
            return this;
        }

        /**
         * Adds a generic {@link AnalyticsPropertyBehavior} to the {@link AnalyticsProperties} under
         * construction. This is an extension point allowing a plugin to handle any other kind of
         * property.
         *
         * @param key A name for the property
         * @param value A property to store
         * @param <T> The type stored in the property
         * @param <P> The type implementing the AnalyticsPropertyBehavior
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public <T, P extends AnalyticsPropertyBehavior<T>> Builder add(@NonNull String key, @NonNull P value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            this.properties.put(key, value);
            return this;
        }

        /**
         * Returns the built {@link AnalyticsProperties}.
         *
         * @return The constructed {@link AnalyticsProperties} configured with the properties set in
         * the Builder
         */
        @NonNull
        public AnalyticsProperties build() {
            return new AnalyticsProperties(this.properties);
        }
    }
}
