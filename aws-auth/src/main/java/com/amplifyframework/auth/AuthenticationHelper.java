package com.amplifyframework.auth;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import kotlin.text.Charsets;

// 💀💀💀 DARK MAGIC. 💀💀💀
// FORCES BEYOND YOUR MORTAL COMPREHENSION ARE IN PLAY. BEWARE.
// This is copied from the AWS Android SDK.
// It's a bunch of signature and SRP tools. Badly needs to be refactored.
@SuppressWarnings("all")
class AuthenticationHelper {
    private BigInteger a;
    private BigInteger A;
    private String poolName;

    public AuthenticationHelper(String userPoolName) {
        do {
            a = new BigInteger(EPHEMERAL_KEY_LENGTH, SECURE_RANDOM).mod(N);
            A = GG.modPow(a, N);
        } while (A.mod(N).equals(BigInteger.ZERO));

        if (userPoolName.contains("_")) {
            poolName = userPoolName.split("_", 2)[1];
        } else {
            poolName = userPoolName;
        }
    }

    public BigInteger geta() {
        return a;
    }

    public BigInteger getA() {
        return A;
    }

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
    private static final BigInteger KK;

    private static final int EPHEMERAL_KEY_LENGTH = 1024;
    private static final int DERIVED_KEY_SIZE = 16;
    private static final String DERIVED_KEY_INFO = "Caldera Derived Key";

    private static final ThreadLocal<MessageDigest> THREAD_MESSAGE_DIGEST = new ThreadLocal<MessageDigest>() {
        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (final NoSuchAlgorithmException e) {
                throw new CognitoInternalErrorException("Exception in authentication", e);
            }
        }
    };

    private static final SecureRandom SECURE_RANDOM;

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");

            final MessageDigest messageDigest = THREAD_MESSAGE_DIGEST.get();
            messageDigest.reset();
            messageDigest.update(N.toByteArray());
            final byte[] digest = messageDigest.digest(GG.toByteArray());
            KK = new BigInteger(1, digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new CognitoInternalErrorException(e.getMessage(), e);
        }
    }

    public byte[] getPasswordAuthenticationKey(String userId,
                                               String userPassword,
                                               BigInteger B,
                                               BigInteger salt) {
        // Authenticate the password
        // u = H(A, B)
        final MessageDigest messageDigest = THREAD_MESSAGE_DIGEST.get();
        messageDigest.reset();
        messageDigest.update(A.toByteArray());
        final BigInteger u = new BigInteger(1, messageDigest.digest(B.toByteArray()));
        if (u.equals(BigInteger.ZERO)) {
            throw new CognitoInternalErrorException("Hash of A and B cannot be zero");
        }

        // x = H(salt | H(poolName | userId | ":" | password))
        messageDigest.reset();
        messageDigest.update(poolName.getBytes(Charsets.UTF_8));
        messageDigest.update(userId.getBytes(Charsets.UTF_8));
        messageDigest.update(":".getBytes(Charsets.UTF_8));
        final byte[] userIdHash = messageDigest.digest(userPassword.getBytes(Charsets.UTF_8));

        messageDigest.reset();
        messageDigest.update(salt.toByteArray());
        final BigInteger x = new BigInteger(1, messageDigest.digest(userIdHash));
        final BigInteger s = (B.subtract(KK.multiply(GG.modPow(x, N)))
            .modPow(a.add(u.multiply(x)), N)).mod(N);

        Hkdf hkdf = null;
        try {
            hkdf = Hkdf.getInstance("HmacSHA256");
        } catch (final NoSuchAlgorithmException e) {
            throw new CognitoInternalErrorException(e.getMessage(), e);
        }
        hkdf.init(s.toByteArray(), u.toByteArray());
        final byte[] key = hkdf.deriveKey(DERIVED_KEY_INFO, DERIVED_KEY_SIZE);
        return key;
    }

    private static class CognitoInternalErrorException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CognitoInternalErrorException(String message, NoSuchAlgorithmException e) {
            super(message, e);
        }

        public CognitoInternalErrorException(String message) {
            super(message);
        }
    }

    public static final class Hkdf {
        private static final byte[] EMPTY_ARRAY = new byte[0];
        private final String algorithm;
        private SecretKey prk = null;
        private static final int MAX_KEY_SIZE = 255;

        /**
         * Returns an new instance
         *
         * @param algorithm the crypto algorithm
         * @return a new instance of {@link Hkdf}
         * @throws NoSuchAlgorithmException
         */
        public static Hkdf getInstance(String algorithm) throws NoSuchAlgorithmException {
            final Mac mac = Mac.getInstance(algorithm);
            return new Hkdf(algorithm);
        }

        /**
         * @param ikm REQUIRED: The input key material.
         */
        public void init(byte[] ikm) {
            this.init(ikm, (byte[]) null);
        }

        /**
         * @param ikm REQUIRED: The input key material.
         * @param salt REQUIRED: Random bytes for salt.
         */
        public void init(byte[] ikm, byte[] salt) {
            byte[] realSalt = salt == null ? EMPTY_ARRAY : salt.clone();
            byte[] rawKeyMaterial = EMPTY_ARRAY;

            try {
                final Mac e = Mac.getInstance(this.algorithm);
                if (realSalt.length == 0) {
                    realSalt = new byte[e.getMacLength()];
                    Arrays.fill(realSalt, (byte) 0);
                }

                e.init(new SecretKeySpec(realSalt, this.algorithm));
                rawKeyMaterial = e.doFinal(ikm);
                final SecretKeySpec key = new SecretKeySpec(rawKeyMaterial, this.algorithm);
                Arrays.fill(rawKeyMaterial, (byte) 0);
                this.unsafeInitWithoutKeyExtraction(key);
            } catch (final GeneralSecurityException var10) {
                throw new RuntimeException("Unexpected exception", var10);
            } finally {
                Arrays.fill(rawKeyMaterial, (byte) 0);
            }

        }

        /**
         * @param rawKey REQUIRED: Current secret key.
         * @throws InvalidKeyException
         */
        public void unsafeInitWithoutKeyExtraction(SecretKey rawKey) throws InvalidKeyException {
            if (!rawKey.getAlgorithm().equals(this.algorithm)) {
                throw new InvalidKeyException(
                    "Algorithm for the provided key must match the algorithm for this Hkdf. Expected "
                        + this.algorithm + " but found " + rawKey.getAlgorithm());
            } else {
                this.prk = rawKey;
            }
        }

        /**
         * @param algorithm REQUIRED: The type of HMAC algorithm to be used.
         */
        private Hkdf(String algorithm) {
            if (!algorithm.startsWith("Hmac")) {
                throw new IllegalArgumentException("Invalid algorithm " + algorithm
                    + ". Hkdf may only be used with Hmac algorithms.");
            } else {
                this.algorithm = algorithm;
            }
        }

        /**
         * @param info REQUIRED
         * @param length REQUIRED
         * @return converted bytes.
         */
        public byte[] deriveKey(String info, int length) {
            return this.deriveKey(info != null ? info.getBytes(Charsets.UTF_8) : null, length);
        }

        /**
         * @param info REQUIRED
         * @param length REQUIRED
         * @return converted bytes.
         */
        public byte[] deriveKey(byte[] info, int length) {
            final byte[] result = new byte[length];

            try {
                this.deriveKey(info, length, result, 0);
                return result;
            } catch (final ShortBufferException var5) {
                throw new RuntimeException(var5);
            }
        }

        /**
         * @param info REQUIRED
         * @param length REQUIRED
         * @param output REQUIRED
         * @param offset REQUIRED
         * @throws ShortBufferException
         */
        public void deriveKey(byte[] info, int length, byte[] output, int offset)
            throws ShortBufferException {
            this.assertInitialized();
            if (length < 0) {
                throw new IllegalArgumentException("Length must be a non-negative value.");
            } else if (output.length < offset + length) {
                throw new ShortBufferException();
            } else {
                final Mac mac = this.createMac();
                if (length > MAX_KEY_SIZE * mac.getMacLength()) {
                    throw new IllegalArgumentException(
                        "Requested keys may not be longer than 255 times the underlying HMAC length.");
                } else {
                    byte[] t = EMPTY_ARRAY;

                    try {
                        int loc = 0;

                        for (byte i = 1; loc < length; ++i) {
                            mac.update(t);
                            mac.update(info);
                            mac.update(i);
                            t = mac.doFinal();

                            for (int x = 0; x < t.length && loc < length; ++loc) {
                                output[loc] = t[x];
                                ++x;
                            }
                        }
                    } finally {
                        Arrays.fill(t, (byte) 0);
                    }

                }
            }
        }

        /**
         * @return the generates message authentication code.
         */
        private Mac createMac() {
            try {
                final Mac ex = Mac.getInstance(this.algorithm);
                ex.init(this.prk);
                return ex;
            } catch (final NoSuchAlgorithmException var2) {
                throw new RuntimeException(var2);
            } catch (final InvalidKeyException var3) {
                throw new RuntimeException(var3);
            }
        }

        /**
         * Checks for a valid pseudo-random key.
         */
        private void assertInitialized() {
            if (this.prk == null) {
                throw new IllegalStateException("Hkdf has not been initialized");
            }
        }
    }
}
