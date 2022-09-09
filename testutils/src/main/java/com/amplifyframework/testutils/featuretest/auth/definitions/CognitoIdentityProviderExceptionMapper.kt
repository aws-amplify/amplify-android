package com.amplifyframework.testutils.featuretest.auth.definitions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amplifyframework.testutils.featuretest.auth.generators.toJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

const val TYPE_FIELD: String = "errorType"
const val MESSAGE_FIELD: String = "errorMessage"

/**
 * Provides mapping between exceptions defined in [aws.sdk.kotlin.services.cognitoidentityprovider.model]
 * instead of using reflection as this will help keeping tight coupling with actual definitions in the test case
 * and test case generators
 */

fun JsonObject.toCognitoException(): CognitoIdentityProviderException {
    val errorType: String? = (this[TYPE_FIELD] as JsonPrimitive).contentOrNull
    val errorMessage: String? = (this[MESSAGE_FIELD] as JsonPrimitive).contentOrNull

    return when (errorType) {
        NotAuthorizedException::class.simpleName -> NotAuthorizedException.invoke { message = errorMessage }
        else -> throw Error("Exception for $errorType is not defined")
    }
}

fun CognitoIdentityProviderException.serialize(): JsonElement {
    val responseMap = mutableMapOf<String, Any?>(
        TYPE_FIELD to this::class.simpleName,
        MESSAGE_FIELD to message
    )

    return responseMap.toJsonElement()
}
