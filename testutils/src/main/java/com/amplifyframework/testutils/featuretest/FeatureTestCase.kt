package com.amplifyframework.testutils.featuretest

import com.amplifyframework.testutils.featuretest.auth.AuthAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * FeatureSpecification for auth feature tests.
 * To generate schema: see [https://github.com/Ricky12Awesome/json-schema-serialization]
 */
@Serializable
data class FeatureTestCase(
    val description: String,
    val preConditions: PreConditions,
    val api: API,
    val validations: List<ExpectationShapes>
)

@Serializable
data class PreConditions(
    val `amplify-configuration`: String,
    val state: String,
    val mockedResponses: List<MockResponse>
)

@Serializable
data class API(
    val name: AuthAPI,
    val params: JsonElement,
    val options: JsonElement
)

@Serializable
data class MockResponse(
    val type: String,
    val apiName: String,
    val responseType: ResponseType,
    val response: JsonElement
)

@Serializable
enum class ResponseType {
    @SerialName("success")
    Success,
    @SerialName("failure")
    Failure
}

@Serializable
sealed class ExpectationShapes {
    @Serializable
    @SerialName("cognito")
    data class Cognito(
        val apiName: String,
        val request: JsonElement
    ) : ExpectationShapes()

    @Serializable
    @SerialName("amplify")
    data class Amplify(
        val apiName: AuthAPI,
        val responseType: ResponseType,
        val response: JsonElement
    ) : ExpectationShapes()

    @Serializable
    @SerialName("state")
    data class State(
        val expectedState: String
    ) : ExpectationShapes()
}
