package featureTest.utilities

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.testutils.featuretest.MockResponse
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Factory to mock aws sdk's cognito API calls and responses.
 */
class CognitoMockFactory(private val mockCognitoIPClient: CognitoIdentityProviderClient) {
    private val captures: MutableMap<String, CapturingSlot<*>> = mutableMapOf()

    fun mock(mockResponse: MockResponse) = when (mockResponse.apiName) {
        "forgotPassword" -> {
            val requestBuilderCaptor = slot<ForgotPasswordRequest.Builder.() -> Unit>()

            coEvery { mockCognitoIPClient.forgotPassword(capture(requestBuilderCaptor)) } coAnswers {
                ForgotPasswordResponse.invoke {
                    this.codeDeliveryDetails = parseCodeDeliveryDetails(mockResponse.response as JsonObject)
                }
            }

            captures[mockResponse.apiName] = requestBuilderCaptor
        }
        "signUp" -> {
            mockkObject(SRPHelper)
            coEvery { SRPHelper.getSecretHash(any(), any(), any()) } returns "a hash"

            val requestCaptor = slot<SignUpRequest.Builder.() -> Unit>()

            coEvery { mockCognitoIPClient.signUp(capture(requestCaptor)) } coAnswers {
                SignUpResponse.invoke {
                    this.codeDeliveryDetails = parseCodeDeliveryDetails(mockResponse.response as JsonObject)
                }
            }
            captures[mockResponse.apiName] = requestCaptor
        }

        else -> throw Error("mock for ${mockResponse.apiName} not defined!")
    }

    private fun parseCodeDeliveryDetails(response: JsonObject): CodeDeliveryDetailsType {
        val codeDeliveryDetails = response["codeDeliveryDetails"] as JsonObject

        return CodeDeliveryDetailsType.invoke {
            destination = (codeDeliveryDetails["destination"] as JsonPrimitive).content
            deliveryMedium =
                DeliveryMediumType.fromValue((codeDeliveryDetails["deliveryMedium"] as JsonPrimitive).content)
            attributeName = (codeDeliveryDetails["attributeName"] as JsonPrimitive).content
        }
    }

    fun getActualResultFor(apiName: String): Any = when (apiName) {
        "forgotPassword" -> {
            val capturedVal = captures[apiName]?.captured as ForgotPasswordRequest.Builder.() -> Unit
            ForgotPasswordRequest.invoke(capturedVal)
        }
        "signUp" -> {
            val capturedVal = captures[apiName]?.captured as SignUpRequest.Builder.() -> Unit
            SignUpRequest.invoke(capturedVal)
        }
        else -> Error("Actual result for $apiName is not defined")
    }
}
