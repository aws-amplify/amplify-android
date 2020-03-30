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

public final class AuthCodeDeliveryDetails {
    private String destination;
    private String deliveryMedium;
    private String attributeName;

    /**
     * Stores information about how the auth code is delivered.
     * @param destination The address the code was sent to
     * @param deliveryMedium What method was used to send the code
     * @param attributeName What attribute was being verified
     */
    public AuthCodeDeliveryDetails(String destination, String deliveryMedium, String attributeName) {
        this.destination = destination;
        this.deliveryMedium = deliveryMedium;
        this.attributeName = attributeName;
    }

    /**
     * Get the address of where the code was sent.
     * @return The address the code was sent to
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Get the method used to send the code.
     * @return The method used to send the code
     */
    public String getDeliveryMedium() {
        return deliveryMedium;
    }

    /**
     * Get the attribute being verified.
     * @return The attribute being verified
     */
    public String getAttributeName() {
        return attributeName;
    }
}
