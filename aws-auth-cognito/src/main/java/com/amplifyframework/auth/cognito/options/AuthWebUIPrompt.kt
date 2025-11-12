package com.amplifyframework.auth.cognito.options

public enum class AuthWebUIPrompt(val value: String) {
    NONE(value = "none"),

    LOGIN(value = "login"),

    SELECT_ACCOUNT(value = "select_account"),

    CONSENT(value = "consent")
}