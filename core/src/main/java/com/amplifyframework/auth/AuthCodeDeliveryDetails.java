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

package com.amplifyframework.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * Wrapper class for representing the various aspects of a confirmation code sent to a user.
 */
public final class AuthCodeDeliveryDetails {
    private String destination;
    private DeliveryMedium deliveryMedium;
    private String attributeName;

    /**
     * Stores information about how a confirmation code is delivered.
     * @param destination The address the code was sent to
     * @param deliveryMedium What method was used to send the code
     */
    public AuthCodeDeliveryDetails(
            @NonNull String destination,
            @NonNull DeliveryMedium deliveryMedium
    ) {
        this(destination, deliveryMedium, null);
    }

    /**
     * Stores information about how a confirmation code is delivered.
     * @param destination The address the code was sent to
     * @param deliveryMedium What method was used to send the code
     * @param attributeName What attribute was being verified, if any
     */
    public AuthCodeDeliveryDetails(
            @NonNull String destination,
            @NonNull DeliveryMedium deliveryMedium,
            @Nullable String attributeName
    ) {
        this.destination = Objects.requireNonNull(destination);
        this.deliveryMedium = Objects.requireNonNull(deliveryMedium);
        this.attributeName = attributeName;
    }

    /**
     * Get the address of where the code was sent.
     * @return The address the code was sent to
     */
    @NonNull
    public String getDestination() {
        return destination;
    }

    /**
     * Get the method used to send the code.
     * @return The method used to send the code
     */
    @NonNull
    public DeliveryMedium getDeliveryMedium() {
        return deliveryMedium;
    }

    /**
     * Get the attribute being verified, if any.
     * @return The attribute being verified, if any
     */
    @Nullable
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getDestination(),
                getDeliveryMedium(),
                getAttributeName()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthCodeDeliveryDetails authCodeDeliveryDetails = (AuthCodeDeliveryDetails) obj;
            return ObjectsCompat.equals(getDestination(), authCodeDeliveryDetails.getDestination()) &&
                    ObjectsCompat.equals(getDeliveryMedium(), authCodeDeliveryDetails.getDeliveryMedium()) &&
                    ObjectsCompat.equals(getAttributeName(), authCodeDeliveryDetails.getAttributeName());
        }
    }

    @Override
    public String toString() {
        return "AuthCodeDeliveryDetails{" +
                "destination='" + destination + '\'' +
                ", deliveryMedium=" + deliveryMedium +
                ", attributeName='" + attributeName + '\'' +
                '}';
    }

    /**
     * The various ways a code could have been sent.
     */
    public enum DeliveryMedium {
        /** Code was sent via email. */
        EMAIL("email"),
        /** Code was sent via text message SMS. */
        SMS("sms"),
        /** Code was sent via phone call. */
        PHONE("phone"),
        /** Code was sent via some other method not listed here. */
        UNKNOWN("unknown");

        private String value;

        DeliveryMedium(@NonNull String value) {
            this.value = Objects.requireNonNull(value);
        }

        /**
         * Get the string value of the enum.
         * @return the string value of the enum.
         */
        @NonNull
        public String getValue() {
            return value;
        }

        /**
         * Get the corresponding ENUM value to the string provided.
         * @param value the string provided.
         * @return the corresponding ENUM value.
         */
        @NonNull
        public static DeliveryMedium fromString(String value) {
            for (DeliveryMedium v : values()) {
                if (v.getValue().equalsIgnoreCase(value)) {
                    return v;
                }
            }

            return DeliveryMedium.UNKNOWN;
        }
    }
}