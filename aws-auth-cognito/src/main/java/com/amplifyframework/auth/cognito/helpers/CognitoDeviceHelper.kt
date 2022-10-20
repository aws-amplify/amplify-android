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

package com.amplifyframework.auth.cognito.helpers

import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID

/**
 * A utility class for device operations.
 */
internal object CognitoDeviceHelper {
    const val SALT_LENGTH_BITS = 128

    /**
     * Generates SRP verification parameters for device verification.
     *
     * @param deviceKey          REQUIRED: Username this device belongs to.
     * @param deviceGroup        REQUIRED: This is the device group id returned by the service.
     * @return srp verification details for this device, as a [Map].
     */
    fun generateVerificationParameters(deviceKey: String, deviceGroup: String): Map<String, String> {
        val deviceSecret = UUID.randomUUID().toString()
        val salt = BigInteger(SALT_LENGTH_BITS, SecureRandom())

        val srpHelper = SRPHelper(deviceSecret)
        srpHelper.setUserPoolParams(deviceKey, deviceGroup)
        val verifier = srpHelper.computePasswordVerifier(salt)

        val params = mutableMapOf<String, String>()
        params["salt"] = android.util.Base64.encodeToString(salt.toByteArray(), android.util.Base64.NO_WRAP)
        params["verifier"] = android.util.Base64.encodeToString(verifier.toByteArray(), android.util.Base64.NO_WRAP)
        params["secret"] = deviceSecret
        return params
    }
}
