/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect.internal

import com.amplifyframework.connect.ConnectValidationException
import com.amplifyframework.connect.UserProfile
import com.amplifyframework.connect.UserProfileLocation
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class InputValidationTest {

    private val maxString = "a".repeat(255)
    private val overMaxString = "a".repeat(256)

    @Test
    fun `email at 255 chars accepted`() {
        shouldNotThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(UserProfile(email = maxString))
        }
    }

    @Test
    fun `email at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(UserProfile(email = overMaxString))
        }
        ex.message shouldContain "email"
    }

    @Test
    fun `name at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(UserProfile(name = overMaxString))
        }
        ex.message shouldContain "name"
    }

    @Test
    fun `phone at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(UserProfile(phone = overMaxString))
        }
        ex.message shouldContain "phone"
    }

    @Test
    fun `customAttributes key at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(customAttributes = mapOf(overMaxString to "v"))
            )
        }
        ex.message shouldContain "key"
    }

    @Test
    fun `customAttributes value at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(customAttributes = mapOf("k" to overMaxString))
            )
        }
        ex.message shouldContain "value"
    }

    @Test
    fun `location city at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(location = UserProfileLocation(city = overMaxString))
            )
        }
        ex.message shouldContain "city"
    }

    @Test
    fun `location country at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(location = UserProfileLocation(country = overMaxString))
            )
        }
        ex.message shouldContain "country"
    }

    @Test
    fun `location postalCode at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(location = UserProfileLocation(postalCode = overMaxString))
            )
        }
        ex.message shouldContain "postalCode"
    }

    @Test
    fun `location region at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(location = UserProfileLocation(region = overMaxString))
            )
        }
        ex.message shouldContain "region"
    }

    @Test
    fun `token at 255 chars accepted`() {
        shouldNotThrow<ConnectValidationException> {
            InputValidation.validateToken(maxString)
        }
    }

    @Test
    fun `token at 256 chars rejected`() {
        val ex = shouldThrow<ConnectValidationException> {
            InputValidation.validateToken(overMaxString)
        }
        ex.message shouldContain "token"
    }

    @Test
    fun `all fields at 255 chars accepted`() {
        shouldNotThrow<ConnectValidationException> {
            InputValidation.validateUserProfile(
                UserProfile(
                    email = maxString,
                    name = maxString,
                    phone = maxString,
                    customAttributes = mapOf(maxString to maxString),
                    location = UserProfileLocation(
                        city = maxString,
                        country = maxString,
                        postalCode = maxString,
                        region = maxString
                    )
                )
            )
        }
    }
}
