package featureTest.json.generators

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.testutils.featuretest.API
import com.amplifyframework.testutils.featuretest.ExpectationShapes
import com.amplifyframework.testutils.featuretest.FeatureSpecification
import com.amplifyframework.testutils.featuretest.MockResponse
import com.amplifyframework.testutils.featuretest.PreConditions
import com.amplifyframework.testutils.featuretest.ResponseType
import com.amplifyframework.testutils.featuretest.auth.AuthAPI
import java.io.FileWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class TestCaseJsonGenerator {

    companion object {
        const val basePath = "/feature-test/testsuites"
    }

    @Test
    fun generateAuthenticationState_SignedIn() {
        val codeDeliveryDetails = mapOf(
            "destination" to "dummy destination",
            "deliveryMedium" to "EMAIL",
            "attributeName" to "dummy attribute"
        )

        val expectedSuccess =
            mapOf(
                "isPasswordReset" to false,
                "nextStep" to
                    mapOf(
                        "resetPasswordStep" to "CONFIRM_RESET_PASSWORD_WITH_CODE",
                        "additionalInfo" to emptyMap<String, String>(),
                        "codeDeliveryDetails" to codeDeliveryDetails
                    )
            ).toJsonElement()

        val validations: List<ExpectationShapes> = listOf(
            ExpectationShapes.Cognito(
                "forgotPassword",
                mapOf(
                    "username" to "someUsername",
                    "clientId" to "testAppClientId",
                    "clientMetadata" to emptyMap<String, String>()
                ).toJsonElement()
            ),
            ExpectationShapes.Amplify(
                AuthAPI.resetPassword,
                ResponseType.Success,
                expectedSuccess,
            ),
            ExpectationShapes.State("AuthenticationState_SignedIn.json")
        )

        val feature = FeatureSpecification(
            description = "Test that Cognito is called with given payload and returns successful data",
            preConditions = PreConditions(
                "amplifyconfiguration.json",
                "AuthenticationState_SignedIn.json",
                mockedResponses = listOf(
                    MockResponse(
                        "cognito",
                        "forgotPassword",
                        ResponseType.Success,
                        mapOf("codeDeliveryDetails" to codeDeliveryDetails).toJsonElement()
                    )
                )
            ),
            api = API(
                AuthAPI.resetPassword,
                mapOf("username" to "someUsername").toJsonElement(),
                JsonObject(emptyMap())
            ),
            validations = validations
        )

        feature.printJson()
    }

    @Test
    fun generateSignUp() {
        val username = "user"
        val password = "password"
        val email = "user@domain.com"

        val codeDeliveryDetails = mapOf(
            "destination" to email,
            "deliveryMedium" to "EMAIL",
            "attributeName" to "attributeName"
        )

        val feature = FeatureSpecification(
            description = "Test that signup invokes proper cognito request and returns success",
            preConditions = PreConditions(
                "amplifyconfiguration.json",
                "AuthenticationState_Configured.json",
                mockedResponses = listOf(
                    MockResponse(
                        "cognito",
                        "signUp",
                        ResponseType.Success,
                        mapOf("codeDeliveryDetails" to codeDeliveryDetails).toJsonElement()
                    )
                )
            ),
            api = API(
                AuthAPI.signUp,
                params = mapOf(
                    "username" to username,
                    "password" to password
                ).toJsonElement(),
                options = mapOf(
                    "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
                ).toJsonElement()
            ),
            validations = listOf(
                ExpectationShapes.Cognito(
                    apiName = "signUp",
                    // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_SignUp.html]
                    request = mapOf(
                        "clientId" to "testAppClientId", // This should be pulled from configuration
                        "username" to username,
                        "password" to password,
                        "userAttributes" to listOf(mapOf("name" to "email", "value" to email))
                    ).toJsonElement()
                ),
                ExpectationShapes.Amplify(
                    apiName = AuthAPI.signUp,
                    responseType = ResponseType.Success,
                    response = mapOf(
                        "isSignUpComplete" to false,
                        "nextStep" to mapOf(
                            "signUpStep" to AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                            "additionalInfo" to emptyMap<String, String>(),
                            "codeDeliveryDetails" to mapOf(
                                "destination" to email,
                                "deliveryMedium" to "EMAIL",
                                "attributeName" to "attributeName"
                            )
                        ),
                        "user" to mapOf(
                            "userId" to "",
                            "username" to username
                        )
                    ).toJsonElement()
                )
            )
        ).printJson()
    }

    fun writeFile(json: String, fileName: String) {
        val basePath = this::class.java.getResource(basePath)
        println("Base path = ${basePath!!.path}")
        val filePath = "${basePath.path}/$fileName"

        val fileWriter = FileWriter(filePath)
        fileWriter.write(json)
        fileWriter.close()
    }
}

private fun FeatureSpecification.printJson() {
    val format = Json {
        prettyPrint = true
    }

    val result = format.encodeToString(this)
    println("Json :\n $result")
}

/**
 * Extension class to convert primitives and collections
 * from [https://github.com/Kotlin/kotlinx.serialization/issues/296#issuecomment-1132714147]
 */
fun Map<*, *>.toJsonElement(): JsonElement {
    return JsonObject(
        mapNotNull {
            (it.key as? String ?: return@mapNotNull null) to it.value.toJsonElement()
        }.toMap()
    )
}

fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        null -> JsonNull
        is Map<*, *> -> toJsonElement()
        is Collection<*> -> toJsonElement()
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }
}
