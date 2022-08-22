package com.amplifyframework.testutils.featuretest.auth

/**
 * List of APIs supported by Auth.
 * Note that case of ENUMs are not capitalized so as to serialze it into proper case
 * something like `resetPassword` instead of `RESET_PASSWORD`
 */
enum class AuthAPI {
    resetPassword,
    signUp;
}
