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

package com.amplifyframework.auth.cognito.helpers;

import com.amazonaws.util.Base64;
import com.amazonaws.util.StringUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A utility class for device operations.
 */
public final class CognitoDeviceHelper {
    private static deviceSRP srpCalculator = null;

    private CognitoDeviceHelper(){}

    /**
     * Generates SRP verification parameters for device verification.
     *
     * @param deviceKey          REQUIRED: Username this device belongs to.
     * @param deviceGroup        REQUIRED: This is the device group id returned by the service.
     * @return srp verification details for this device, as a {@link Map}.
     */
    public static Map<String, String> generateVerificationParameters(String deviceKey, String deviceGroup) {
        final Map<String, String> devVerfPars = new HashMap<String, String>();
        final String deviceSecret = generateRandomString();
        srpCalculator = new deviceSRP(deviceGroup, deviceKey, deviceSecret);
        final byte[] salt = srpCalculator.getSalt().toByteArray();
        final byte[] srpVerifier = srpCalculator.getVerifier().toByteArray();
        devVerfPars.put("salt", new String(Base64.encode(salt)));
        devVerfPars.put("verifier", new String(Base64.encode(srpVerifier)));
        devVerfPars.put("secret", deviceSecret);
        return devVerfPars;
    }

    /**
     * Returns a string with random characters.
     *
     * @return a string with random alpha-numeric characters.s
     */
    private static String generateRandomString() {
        final UUID uuid = UUID.randomUUID();
        return String.valueOf(uuid);
    }

    /**
     * Static class for SRP related calculations for devices.
     */
    @SuppressWarnings("checkstyle:typename")
    private static final class deviceSRP {
        private static final String HASH_ALGORITHM = "SHA-256";
        private static final ThreadLocal<MessageDigest> THREAD_MESSAGE_DIGEST =
            new ThreadLocal<MessageDigest>() {
                @Override
                protected MessageDigest initialValue() {
                    try {
                        return MessageDigest.getInstance(HASH_ALGORITHM);
                    } catch (final NoSuchAlgorithmException exception) {
                        throw new ExceptionInInitializerError(exception);
                    }
                }
            };

        private static final BigInteger N = new BigInteger(SRPHelper.getHexN(), 16);
        private static final BigInteger GG = BigInteger.valueOf(2);
        private static final int SALT_LENGTH_BITS = 128;

        private static final SecureRandom SECURE_RANDOM;

        private final BigInteger salt;
        private final BigInteger verifier;

        static {
            try {
                SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
            } catch (final NoSuchAlgorithmException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        /**
         * Helps to start the SRP validation of the device.
         * @param deviceGroupKey REQUIRED: Group assigned to the device.
         * @param deviceKey REQUIRED: Unique identifier assigned to the device.
         * @param password REQUIRED: The device password.
         */
        private deviceSRP(String deviceGroupKey, String deviceKey, String password) {
            final byte[] deviceKeyHash = getUserIdHash(deviceGroupKey, deviceKey, password);

            salt = new BigInteger(SALT_LENGTH_BITS, SECURE_RANDOM);
            verifier = calcVerifier(salt, deviceKeyHash);
        }

        /**
         * Getter for salt.
         * @return salt
         */
        private BigInteger getSalt() {
            return salt;
        }

        /**
         * Returns the generated verifier.
         * @return verifier.
         */
        private BigInteger getVerifier() {
            return verifier;
        }

        /**
         * Generates the SRP verifier.
         * @param salt REQUIRED: The random salt created by the service.
         * @param userIdHash REQIURED: Username hash.
         * @return verifier as a BigInteger.
         */
        private static BigInteger calcVerifier(BigInteger salt, byte[] userIdHash) {
            begin();
            update(salt);
            update(userIdHash);
            final byte[] digest = end();

            final BigInteger x = new BigInteger(1, digest);
            return GG.modPow(x, N);
        }

        /**
         * Computes the user hash.
         * @param poolName REQUIRED: The pool-id of the user.
         * @param userName REQUIRED: The internal username of the user.
         * @param password REQUIRED: The password intered by the user.
         * @return hash as a byte array.
         */
        private byte[] getUserIdHash(String poolName, String userName, String password) {
            begin();
            update(poolName, userName, ":", password);
            return end();
        }

        /**
         * Start byte digest for SRP.
         */
        private static void begin() {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            md.reset();
        }

        /**
         * Complete digest.
         * @return the digest as a byte array.
         */
        private static byte[] end() {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            return md.digest();
        }

        /**
         * Adds a series of strings to the digest.
         * @param strings REQUIRED: Strings to add.
         */
        private static void update(String... strings) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            for (final String s : strings) {
                if (s != null) {
                    md.update(s.getBytes(StringUtils.UTF8));
                }
            }
        }

        /**
         * Adds a BigInteger to the digest.
         * @param number REQUIRED: The number to add.
         */
        private static void update(BigInteger number) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            if (number != null) {
                md.update(number.toByteArray());
            }
        }

        /**
         * Adds a byte array to the digest.
         * @param bytes REQUIRED: bytes to add.
         */
        private static void update(byte[] bytes) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            if (bytes != null) {
                md.update(bytes);
            }
        }
    }
}
