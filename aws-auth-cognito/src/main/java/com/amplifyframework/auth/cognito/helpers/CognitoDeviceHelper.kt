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

import com.amplifyframework.auth.cognito.helpers.SRPHelper.Companion.getHexN
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * A utility class for device operations.
 */
object CognitoDeviceHelper {
    /**
     * Generates SRP verification parameters for device verification.
     *
     * @param deviceKey          REQUIRED: Username this device belongs to.
     * @param deviceGroup        REQUIRED: This is the device group id returned by the service.
     * @return srp verification details for this device, as a [Map].
     */
    fun generateVerificationParameters(deviceKey: String, deviceGroup: String): Map<String, String> {
        val devVerfPars: MutableMap<String, String> = HashMap()
        val deviceSecret = UUID.randomUUID().toString()
        val srpCalculator = DeviceSRP(deviceGroup, deviceKey, deviceSecret)
        val salt: ByteArray = srpCalculator.salt.toByteArray()
        val srpVerifier: ByteArray = srpCalculator.verifier.toByteArray()
        devVerfPars["salt"] = String(android.util.Base64.decode(salt, android.util.Base64.NO_WRAP))
        devVerfPars["verifier"] = String(android.util.Base64.decode(srpVerifier, android.util.Base64.NO_WRAP))
        devVerfPars["secret"] = deviceSecret
        return devVerfPars
    }

    /**
     * Static class for SRP related calculations for devices.
     * Helps to start the SRP validation of the device.
     * @param deviceGroupKey REQUIRED: Group assigned to the device.
     * @param deviceKey REQUIRED: Unique identifier assigned to the device.
     * @param password REQUIRED: The device password.
     */
    private class DeviceSRP constructor(deviceGroupKey: String, deviceKey: String, password: String) {
        val deviceKeyHash = getUserIdHash(deviceGroupKey, deviceKey, password)
        val salt = BigInteger(SALT_LENGTH_BITS, SECURE_RANDOM)
        val verifier = calcVerifier(salt, deviceKeyHash)

        companion object {
            private val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

            private val N = BigInteger(getHexN(), 16)
            private val GG = BigInteger.valueOf(2)

            private const val SALT_LENGTH_BITS = 128
            private val SECURE_RANDOM = SecureRandom()

            /**
             * Generates the SRP verifier.
             * @param salt REQUIRED: The random salt created by the service.
             * @param userIdHash REQIURED: Username hash.
             * @return verifier as a BigInteger.
             */
            private fun calcVerifier(salt: BigInteger, userIdHash: ByteArray): BigInteger {
                digest.reset()
                digest.update(salt.toByteArray())
                digest.update(userIdHash)
                val x = BigInteger(1, digest.digest())
                return GG.modPow(x, N)
            }
        }

        /**
         * Computes the user hash.
         * @param poolName REQUIRED: The pool-id of the user.
         * @param userName REQUIRED: The internal username of the user.
         * @param password REQUIRED: The password intered by the user.
         * @return hash as a byte array.
         */
        private fun getUserIdHash(poolName: String, userName: String, password: String): ByteArray {
            digest.reset()
            digest.update(poolName.toByteArray())
            digest.update(userName.toByteArray())
            digest.update(":".toByteArray())
            digest.update(password.toByteArray())
            return digest.digest()
        }
    }
}
