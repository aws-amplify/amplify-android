/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.maplibre.util

import com.amplifyframework.geo.location.models.AmazonLocationPlace

/**
 * Interface that can be used to customize the address rendering.
 */
interface AddressFormatter {

    /**
     * Format an address string from the place object.
     * @param place the `AmazonLocationPlace` instance
     * @return the formatted address
     */
    fun formatAddress(place: AmazonLocationPlace): String

    /**
     * Format a name string from the place object.
     * @param place the `AmazonLocationPlace` instance
     * @return the formatted name
     */
    fun formatName(place: AmazonLocationPlace): String

}

/**
 * The default address formatter attempts to do a simple formatting of addresses
 * regardless of the place locale configuration. For more accurate address formatting,
 * implement your own [AddressFormatter].
 */
object DefaultAddressFormatter : AddressFormatter {

    /**
     * Joins some address data in a 2-line string format, cleaning up `null` values when found.
     */
    override fun formatAddress(place: AmazonLocationPlace): String {
        val addressLine1 = listOfNotNull(place.addressNumber, place.street)
            .joinToString(", ")
        val addressLine2 = listOfNotNull(place.municipality, place.region, place.postalCode)
            .joinToString(", ")
        return "$addressLine1\n$addressLine2"
    }

    /**
     * Attempts to cleanup the `place.label` string when it contains address information.
     */
    override fun formatName(place: AmazonLocationPlace): String {
        var name = place.label ?: ""
        val indexOfAddressNumber = name.indexOf(", ${place.addressNumber}")
        val indexOfAddressStreet = name.indexOf(", ${place.street}")
        val indexOfAddressCity = name.indexOf(", ${place.municipality}")
        when {
            indexOfAddressNumber != -1 -> {
                name = name.substring(0 until indexOfAddressNumber)
            }
            indexOfAddressStreet != -1 -> {
                name = name.substring(0 until indexOfAddressStreet)
            }
            indexOfAddressCity != -1 -> {
                name = name.substring(0 until indexOfAddressCity)
            }
        }
        return name
    }

}