/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.options

import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import org.junit.Assert
import org.junit.Test

class APIOptionsKotlinContractTest {
    private var metadata: Map<String, String> = mapOf("testKey" to "testValue")

    @Test
    fun testCognitoOptions() {
        val resendUserAttributeConfirmationCodeOptions =
            AWSCognitoAuthResendUserAttributeConfirmationCodeOptions.builder().metadata(metadata).build()
        Assert.assertEquals(resendUserAttributeConfirmationCodeOptions.metadata, metadata)

        val confirmResetPasswordOptions = AWSCognitoAuthConfirmResetPasswordOptions.builder().metadata(metadata).build()
        Assert.assertEquals(confirmResetPasswordOptions.metadata, metadata)

        val confirmSignInOptions = AWSCognitoAuthConfirmSignInOptions.builder().metadata(metadata).build()
        Assert.assertEquals(confirmSignInOptions.metadata, metadata)

        val confirmSignUpOptions = AWSCognitoAuthConfirmSignUpOptions.builder().clientMetadata(metadata).build()
        Assert.assertEquals(confirmSignUpOptions.clientMetadata, metadata)

        val resendSignUpCodeOptions = AWSCognitoAuthResendSignUpCodeOptions.builder().metadata(metadata).build()
        Assert.assertEquals(resendSignUpCodeOptions.metadata, metadata)

        val resetPasswordOptions = AWSCognitoAuthResetPasswordOptions.builder().metadata(metadata).build()
        Assert.assertEquals(resetPasswordOptions.metadata, metadata)

        val signInOptions = AWSCognitoAuthSignInOptions.builder().metadata(metadata).build()
        Assert.assertEquals(signInOptions.metadata, metadata)

        val signOutOptions = AWSCognitoAuthSignOutOptions.builder().browserPackage("chrome").build()
        Assert.assertEquals(signOutOptions.browserPackage, "chrome")

        val attributes = listOf(
            AuthUserAttribute(AuthUserAttributeKey.email(), "my@email.com"),
            AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+15551234567")
        )
        val signUpOptions = AWSCognitoAuthSignUpOptions.builder().clientMetadata(metadata)
            .userAttributes(attributes).build()
        Assert.assertEquals(signUpOptions.clientMetadata, metadata)
        Assert.assertEquals(signUpOptions.userAttributes, attributes)

        val updateUserAttributeOptions = AWSCognitoAuthUpdateUserAttributeOptions.builder().metadata(metadata).build()
        Assert.assertEquals(updateUserAttributeOptions.metadata, metadata)

        val updateUserAttributesOptions = AWSCognitoAuthUpdateUserAttributesOptions.builder().metadata(metadata).build()
        Assert.assertEquals(updateUserAttributesOptions.metadata, metadata)

        val scopes = listOf("name")
        val webUISignInOptions = AWSCognitoAuthWebUISignInOptions.builder().browserPackage("chrome")
            .scopes(scopes).build()
        Assert.assertEquals(webUISignInOptions.browserPackage, "chrome")
        Assert.assertEquals(webUISignInOptions.scopes, scopes)

        val federateToIdentityPoolOptions = FederateToIdentityPoolOptions.builder()
            .developerProvidedIdentityId("test-idp")
            .build()
        Assert.assertEquals(federateToIdentityPoolOptions.developerProvidedIdentityId, "test-idp")
    }

    @Test
    fun testInlineBuilderCognitoOptions() {
        val resendUserAttributeConfirmationCodeOptions = AWSCognitoAuthResendUserAttributeConfirmationCodeOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(resendUserAttributeConfirmationCodeOptions.metadata, metadata)

        val confirmResetPasswordOptions = AWSCognitoAuthConfirmResetPasswordOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(confirmResetPasswordOptions.metadata, metadata)

        val confirmSignInOptions = AWSCognitoAuthConfirmSignInOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(confirmSignInOptions.metadata, metadata)

        val confirmSignUpOptions = AWSCognitoAuthConfirmSignUpOptions {
            this.clientMetadata(metadata)
        }
        Assert.assertEquals(confirmSignUpOptions.clientMetadata, metadata)

        val resendSignUpCodeOptions = AWSCognitoAuthResendSignUpCodeOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(resendSignUpCodeOptions.metadata, metadata)

        val resetPasswordOptions = AWSCognitoAuthResetPasswordOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(resetPasswordOptions.metadata, metadata)

        // TODO: add inline builders
//        val signInOptions = AWSCognitoAuthSignInOptions {
//            this.metadata(metadata)
//        }
//        Assert.assertEquals(signInOptions.metadata, metadata)

//        val signOutOptions = AWSCognitoAuthSignOutOptions.builder().browserPackage("chrome").build()
//        Assert.assertEquals(signOutOptions.browserPackage, "chrome")

        val attributes = listOf(
            AuthUserAttribute(AuthUserAttributeKey.email(), "my@email.com"),
            AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+15551234567")
        )
        val signUpOptions = AWSCognitoAuthSignUpOptions {
            this.clientMetadata(metadata)
            this.userAttributes(attributes)
        }
        Assert.assertEquals(signUpOptions.clientMetadata, metadata)
        Assert.assertEquals(signUpOptions.userAttributes, attributes)

        val updateUserAttributeOptions = AWSCognitoAuthUpdateUserAttributeOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(updateUserAttributeOptions.metadata, metadata)

        val updateUserAttributesOptions = AWSCognitoAuthUpdateUserAttributesOptions {
            this.metadata(metadata)
        }
        Assert.assertEquals(updateUserAttributesOptions.metadata, metadata)

        // TODO: add inline builders
//        val scopes = listOf("name")
//        val webUISignInOptions = AWSCognitoAuthWebUISignInOptions.builder().browserPackage("chrome")
//            .scopes(scopes).build()
//        Assert.assertEquals(webUISignInOptions.browserPackage, "chrome")
//        Assert.assertEquals(webUISignInOptions.scopes, scopes)

        val federateToIdentityPoolOptions = FederateToIdentityPoolOptions {
            this.developerProvidedIdentityId("test-idp")
        }
        Assert.assertEquals(federateToIdentityPoolOptions.developerProvidedIdentityId, "test-idp")
    }
}
