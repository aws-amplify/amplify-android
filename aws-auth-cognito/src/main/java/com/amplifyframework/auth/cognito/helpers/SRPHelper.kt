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

import androidx.annotation.VisibleForTesting
import org.jetbrains.annotations.TestOnly
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Kotlin implementation of SRP crypto calculations.
 * This implementation is a direct translation of
 * AWS Android SDK SRP crypto code into Kotlin.
 * https://github.com/aws-amplify/aws-sdk-android/blob/main/aws-android-sdk-cognitoidentityprovider/src/main/java/com/amazonaws/mobileconnectors/cognitoidentityprovider/CognitoUser.java#L3587
 * SRP requires Kotlin version 1.5+, and minSDK version 24.
 */
class SRPHelper(private val password: String = "") {

    companion object {
        private val EPHEMERAL_KEY_LENGTH = 1024
        private val HMAC_SHA_256 = "HmacSHA256"
    }

    // Generator 'g' parameter.
    private val g = BigInteger.valueOf(2)

    // Precomputed safe 3072-bit prime 'N', as decimal.
    // https://datatracker.ietf.org/doc/html/rfc5054#appendix-A (Page 16)
    private val HEX_N =
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A" +
            "431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5" +
            "AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62" +
            "F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2" +
            "EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D0450" +
            "7A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619D" +
            "CEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E2" +
            "4FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF"
    private val N = BigInteger(HEX_N, 16)
    private val random = SecureRandom()

    private val DERIVED_KEY_INFO = "Caldera Derived Key"
    private val DERIVED_KEY_SIZE = 16

    private val k: BigInteger
    private var privateA: BigInteger
    private var publicA: BigInteger
    var dateString: String

    private val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

    init {
        // Generate client private 'a' and public 'A' values
        do {
            privateA = BigInteger(EPHEMERAL_KEY_LENGTH, random).mod(N)
            // A = (g ^ a) % N
            publicA = g.modPow(privateA, N)
        } while (publicA.mod(N) == BigInteger.ZERO)

        // compute k = H(g|N)
        digest.reset()
        digest.update(N.toByteArray())
        k = BigInteger(1, digest.digest(g.toByteArray()))

        val simpleDateFormat = SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        dateString = simpleDateFormat.format(Date())
    }

    private var userId: String = ""
    private var userPoolName: String = ""

    fun setUserPoolParams(userId: String, userPoolName: String) {
        this.userId = userId
        this.userPoolName = userPoolName.split(Regex("_"), 2)[1]
    }

    @TestOnly
    fun modN(value: BigInteger): BigInteger {
        return value.mod(N)
    }

    @TestOnly
    fun setAValues(privateA: BigInteger, publicA: BigInteger) {
        this.privateA = privateA
        this.publicA = publicA
    }

    fun getPublicA(): String {
        return publicA.toString(16)
    }

    // u = H(A, B)
    // @VisibleForTesting
    internal fun computeU(srpB: BigInteger): BigInteger {
        digest.reset()
        digest.update(publicA.toByteArray())
        return BigInteger(1, digest.digest(srpB.toByteArray()))
    }

    // x = H(salt | H(poolName | userId | ":" | password))
    internal fun computeX(salt: BigInteger): BigInteger {
        digest.reset()
        digest.update(userPoolName.toByteArray())
        digest.update(userId.toByteArray())
        digest.update(":".toByteArray())
        val userIdPasswordHash = digest.digest(password.toByteArray())

        digest.reset()
        digest.update(salt.toByteArray())
        return BigInteger(1, digest.digest(userIdPasswordHash))
    }

    // s = ((B - k * (g ^ x) % N) ^ (a + u * x) % N) % N
    internal fun computeS(uValue: BigInteger, xValue: BigInteger, srpB: BigInteger): BigInteger {
        return (
            srpB.subtract(k.multiply(g.modPow(xValue, N))).modPow(
                privateA.add(uValue.multiply(xValue)),
                N
            )
            ).mod(N)
    }

    // p = MAC("Caldera Derived Key" | 1, MAC(s, u))[0:16]
    internal fun computePasswordAuthenticationKey(ikm: BigInteger, salt: BigInteger): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA_256)
        var keySpec = SecretKeySpec(salt.toByteArray(), HMAC_SHA_256)
        mac.init(keySpec)
        val prk = mac.doFinal(ikm.toByteArray())

        mac.reset()
        keySpec = SecretKeySpec(prk, HMAC_SHA_256)
        mac.init(keySpec)
        mac.update(DERIVED_KEY_INFO.toByteArray())
        val hkdf = mac.doFinal(Char(1).toString().toByteArray())
        return hkdf.copyOf(DERIVED_KEY_SIZE)
    }

    // M1 = MAC(poolId | userId | secret | timestamp, key)
    @VisibleForTesting
    internal fun generateM1Signature(key: ByteArray, secretBlock: String): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA_256)
        val keySpec = SecretKeySpec(key, HMAC_SHA_256)
        mac.init(keySpec)
        mac.update(userPoolName.toByteArray())
        mac.update(userId.toByteArray())
        mac.update(android.util.Base64.decode(secretBlock, android.util.Base64.NO_WRAP))
        return mac.doFinal(dateString.toByteArray())
    }

    fun getSignature(salt: String, srpB: String, secretBlock: String): String {
        val bigIntSRPB = BigInteger(srpB, 16)
        val bigIntSalt = BigInteger(salt, 16)

        // Check B's validity
        if (bigIntSRPB.mod(N) == BigInteger.ZERO)
            throw Exception("Bad server public value 'B'")

        val uValue = computeU(bigIntSRPB)
        if (uValue.mod(N) == BigInteger.ZERO)
            throw Exception("Hash of A and B cannot be zero")

        val xValue = computeX(bigIntSalt)
        val sValue = computeS(uValue, xValue, bigIntSRPB)
        val key = computePasswordAuthenticationKey(sValue, uValue)
        val m1Signature = generateM1Signature(key, secretBlock)
        return String(android.util.Base64.encode(m1Signature, android.util.Base64.NO_WRAP))
    }
}
