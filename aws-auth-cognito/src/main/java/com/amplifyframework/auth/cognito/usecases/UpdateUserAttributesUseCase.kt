/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.updateUserAttributes
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributesOptions
import com.amplifyframework.auth.cognito.requireAccessToken
import com.amplifyframework.auth.cognito.requireSignedInState
import com.amplifyframework.auth.cognito.util.toAuthCodeDeliveryDetails
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.auth.result.step.AuthNextUpdateAttributeStep
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep

internal class UpdateUserAttributesUseCase(
    private val client: CognitoIdentityProviderClient,
    private val fetchAuthSession: FetchAuthSessionUseCase,
    private val stateMachine: AuthStateMachine
) {
    // Update multiple attributes at once
    suspend fun execute(
        attributes: List<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions = AuthUpdateUserAttributesOptions.defaults()
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult> {
        val metadata = (options as? AWSCognitoAuthUpdateUserAttributesOptions)?.metadata
        return updateAttributes(attributes, metadata)
    }

    // Update a single attribute
    suspend fun execute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions = AuthUpdateUserAttributeOptions.defaults()
    ): AuthUpdateAttributeResult {
        val metadata = (options as? AWSCognitoAuthUpdateUserAttributeOptions)?.metadata
        val result = updateAttributes(listOf(attribute), metadata)
        return result.values.first()
    }

    private suspend fun updateAttributes(
        attributes: List<AuthUserAttribute>,
        metadata: Map<String, String>?
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult> {
        stateMachine.requireSignedInState()

        val token = fetchAuthSession.execute().requireAccessToken()

        val response = client.updateUserAttributes {
            accessToken = token
            clientMetadata = metadata
            userAttributes = attributes.map {
                AttributeType {
                    name = it.key.keyString
                    value = it.value
                }
            }
        }

        return response.mapResults(attributes)
    }

    private fun UpdateUserAttributesResponse.mapResults(
        attributes: List<AuthUserAttribute>
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult> = buildMap {
        // Place the specific values returned in the result
        codeDeliveryDetailsList?.forEach { item ->
            item.attributeName?.let {
                val key = AuthUserAttributeKey.custom(it)
                val nextStep = AuthNextUpdateAttributeStep(
                    AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE,
                    HashMap(),
                    item.toAuthCodeDeliveryDetails()
                )
                val result = AuthUpdateAttributeResult(false, nextStep)
                put(key, result)
            }
        }

        // Set any missing attributes to "DONE"
        val doneStep = AuthNextUpdateAttributeStep(
            AuthUpdateAttributeStep.DONE,
            HashMap(),
            null
        )
        val doneResult = AuthUpdateAttributeResult(true, doneStep)
        attributes.forEach { attribute ->
            if (!contains(attribute.key)) {
                put(attribute.key, doneResult)
            }
        }
    }
}
