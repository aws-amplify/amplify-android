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

import android.os.Build;

import com.amazonaws.util.Base64;
import com.amazonaws.util.StringUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
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
     * Uses the Android class {@link Build} to return the model of
     * the android device.
     *
     * @return Device model name, which is also the name of the device.
     */
    public static String getDeviceName() {
        return Build.MODEL;
    }

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
    public static String generateRandomString() {
        final UUID uuid = UUID.randomUUID();
        return String.valueOf(uuid);
    }

    /**
     * Static class for SRP related calculations for devices.
     */
    @SuppressWarnings("checkstyle:typename")
    public static class deviceSRP {
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

        private static final String HEX_N = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
                + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
                + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
                + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
                + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
                + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
                + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
                + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
                + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
                + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
                + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
                + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
                + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
                + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
                + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
                + "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF";

        private static final BigInteger N = new BigInteger(HEX_N, 16);
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
        public deviceSRP(String deviceGroupKey, String deviceKey, String password) {
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
        public BigInteger getVerifier() {
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
        public static void begin() {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            md.reset();
        }

        /**
         * Complete digest.
         * @return the digest as a byte array.
         */
        public static byte[] end() {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            return md.digest();
        }

        /**
         * Adds a series of strings to the digest.
         * @param strings REQUIRED: Strings to add.
         */
        public static void update(String... strings) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            for (final String s : strings) {
                if (s != null) {
                    md.update(s.getBytes(StringUtils.UTF8));
                }
            }
        }

        /**
         * Adds a string to the digest.
         * @param stringToAdd REQUIRED: String to add.
         */
        public static void update(String stringToAdd) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            if (stringToAdd != null) {
                md.update(stringToAdd.getBytes(StringUtils.UTF8));
            }
        }

        /**
         * Adds a series of BigIntegers to the digest.
         * @param bigInts REQUIRED: Numbers to add.
         */
        public static void update(BigInteger... bigInts) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            for (final BigInteger n : bigInts) {
                if (n != null) {
                    md.update(n.toByteArray());
                }
            }
        }

        /**
         * Adds a BigInteger to the digest.
         * @param number REQUIRED: The number to add.
         */
        public static void update(BigInteger number) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            if (number != null) {
                md.update(number.toByteArray());
            }
        }

        /**
         * Adds the contents of a byte-buffer to the digest.
         * @param byteBuffer REQUIRED: bytes to add.
         */
        public static void update(ByteBuffer byteBuffer) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            if (byteBuffer != null) {
                md.update(byteBuffer.array());
            }
        }

        /**
         * Adds a byte array to the digest.
         * @param bytes REQUIRED: bytes to add.
         */
        public static void update(byte[] bytes) {
            final MessageDigest md = THREAD_MESSAGE_DIGEST.get();
            if (bytes != null) {
                md.update(bytes);
            }
        }
    }
}
