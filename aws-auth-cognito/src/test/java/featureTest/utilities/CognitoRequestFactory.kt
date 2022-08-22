package featureTest.utilities

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.testutils.featuretest.ExpectationShapes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Factory to generate request object for aws SDK's cognito APIs
 */
object CognitoRequestFactory {
    fun getExpectedRequestFor(targetApi: ExpectationShapes.Cognito): Any = when (targetApi.apiName) {
        "forgotPassword" -> {
            val params = targetApi.request as JsonObject
            val expectedRequestBuilder: ForgotPasswordRequest.Builder.() -> Unit = {
                username = (params["username"] as JsonPrimitive).content
                clientMetadata =
                    Json.decodeFromJsonElement<Map<String, String>>(params["clientMetadata"] as JsonObject)
                clientId = (params["clientId"] as JsonPrimitive).content
            }

            ForgotPasswordRequest.invoke(expectedRequestBuilder)
        }

        "signUp" -> {
            val params = targetApi.request as JsonObject
            val expectedRequest: SignUpRequest.Builder.() -> Unit = {
                clientId = (params["clientId"] as JsonPrimitive).content
                username = (params["username"] as JsonPrimitive).content
                password = (params["password"] as JsonPrimitive).content

                /*
                 * "userAttributes": [
                      {
                        "name": "email",
                        "value": "user@domain.com"
                      }
                    ]
                 */
                userAttributes = (params["userAttributes"] as JsonArray).mapNotNull {
                    val entry = it as JsonObject
                    AttributeType {
                        name = (entry["name"] as JsonPrimitive).content
                        value = (entry["value"] as JsonPrimitive).content
                    }
                }
                secretHash = SRPHelper.getSecretHash("", "", "")
            }
            SignUpRequest.invoke(expectedRequest)
        }

        else -> throw Error("Expected request for $targetApi for Cognito is not defined")
    }
}
