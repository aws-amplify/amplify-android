package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.MFAType
import kotlin.jvm.Throws

@Throws(IllegalArgumentException::class)
internal fun getMFAType(value: String): MFAType {
    return when (value) {
        "SMS_MFA" -> MFAType.SMS
        "SOFTWARE_TOKEN_MFA" -> MFAType.TOTP
        else -> throw IllegalArgumentException("Unsupported MFA type")
    }
}

internal val MFAType.value: String
    get() = when (this) {
        MFAType.SMS -> "SMS_MFA"
        MFAType.TOTP -> "SOFTWARE_TOKEN_MFA"
    }
