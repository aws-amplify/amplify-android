/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthCodeDeliveryDetails.DeliveryMedium
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should

fun haveAttributeName(name: String) = Matcher<AuthCodeDeliveryDetails> { value ->
    MatcherResult(
        value.attributeName == name,
        { "attribute name should be $name but was ${value.attributeName}" },
        { "attribute name should not be $name but it was" }
    )
}

fun haveDestination(destination: String) = Matcher<AuthCodeDeliveryDetails> { value ->
    MatcherResult(
        value.destination == destination,
        { "destination should be $destination but was ${value.destination}" },
        { "destination should not be $destination but it was" }
    )
}

fun haveDeliveryMedium(medium: DeliveryMedium) = Matcher<AuthCodeDeliveryDetails> { value ->
    MatcherResult(
        value.deliveryMedium == medium,
        { "delivery medium should be $medium but was ${value.deliveryMedium}" },
        { "delivery medium should not be $medium but it was" }
    )
}

infix fun AuthCodeDeliveryDetails.shouldHaveAttributeName(name: String) = apply { this should haveAttributeName(name) }
infix fun AuthCodeDeliveryDetails.shouldHaveDestination(destination: String) = apply {
    this should haveDestination(destination)
}
infix fun AuthCodeDeliveryDetails.shouldHaveDeliveryMedium(medium: DeliveryMedium) = apply {
    this should haveDeliveryMedium(medium)
}
