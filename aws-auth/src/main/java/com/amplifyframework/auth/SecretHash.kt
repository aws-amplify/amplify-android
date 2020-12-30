package com.amplifyframework.auth

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SecretHash {
    companion object {
        fun of(username: String, clientId: String, clientSecret: String): String {
            android.util.Log.w("Hash inputs", "username=${username}, clientId=${clientId}, clientSecret=${clientSecret}")
            val mac = Mac.getInstance("HmacSHA256")
            val secretBytes = clientSecret.toByteArray()
            mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
            val dataBytes = (username + clientId).toByteArray()
            val rawHmac = mac.doFinal(dataBytes)
            val hashString = Base64.encodeToString(rawHmac, Base64.NO_WRAP)
            android.util.Log.w("Hash outputs", hashString)
            return hashString
        }
    }
}
