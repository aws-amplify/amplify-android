package com.amplifyframework.statemachine.codegen.data

data class DeviceMetaData(
    val idToken: String,
    val refreshToken: String,
    val accessToken: String,
    val deviceKey: String,
    val deviceGroupKey: String,
    val userId: String,
    val username: String,
    val expiresIn: Int
)
