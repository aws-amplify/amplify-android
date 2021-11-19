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