/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import kotlin.test.assertEquals
import org.junit.Test

/**
 * Tests for [CodeDeliveryDetailsType.toAuthCodeDeliveryDetails] extension
 */
class CodeDeliveryDetailsTypeExtensionTest {

    @Test(expected = IllegalArgumentException::class)
    fun `CodeDeliveryDetailsType cannot be converted to AuthCodeDeliveryDetails without destination`() {
        // GIVEN
        val deliveryDetailsType = CodeDeliveryDetailsType.invoke {
            deliveryMedium = DeliveryMediumType.Email
            attributeName = "dummy attribute"
        }

        // WHEN
        deliveryDetailsType.toAuthCodeDeliveryDetails()
    }

    @Test
    fun `CodeDeliveryDetailsType maps to AuthCodeDeliveryDetails`() {
        // GIVEN
        val deliveryDetailsType = CodeDeliveryDetailsType.invoke {
            deliveryMedium = DeliveryMediumType.Email
            attributeName = "dummy attribute"
            destination = "dummy destination"
        }
        val expectedResult = AuthCodeDeliveryDetails(
            deliveryDetailsType.destination.toString(),
            AuthCodeDeliveryDetails.DeliveryMedium.fromString(DeliveryMediumType.Email.toString()),
            deliveryDetailsType.attributeName
        )

        // WHEN
        val authCodeDeliveryDetails = deliveryDetailsType.toAuthCodeDeliveryDetails()

        // THEN
        assertEquals(expectedResult, authCodeDeliveryDetails)
    }
}
