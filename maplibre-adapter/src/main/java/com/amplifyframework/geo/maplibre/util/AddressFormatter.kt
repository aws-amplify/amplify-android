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

interface AddressFormatter {

    fun formatAddress(place: AmazonLocationPlace): String

    fun formatName(place: AmazonLocationPlace): String

}

object DefaultAddressFormatter : AddressFormatter {
    override fun formatAddress(place: AmazonLocationPlace): String {
        return "${place.addressNumber}, ${place.street}\n" +
                "${place.municipality}, ${place.region}, ${place.postalCode}"
    }

    override fun formatName(place: AmazonLocationPlace): String {
        var name = place.label ?: ""
        val indexOfAddressNumber = name.indexOf(", ${place.addressNumber}")
        val indexOfAddressStreet = name.indexOf(", ${place.street}")
        if (indexOfAddressNumber != -1) {
            name = name.substring(0 until indexOfAddressNumber)
        } else if (indexOfAddressStreet != -1) {
            name = name.substring(0 until indexOfAddressStreet)
        }
        return name
    }

}