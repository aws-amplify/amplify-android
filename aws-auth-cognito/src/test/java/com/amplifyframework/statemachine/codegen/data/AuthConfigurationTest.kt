package com.amplifyframework.statemachine.codegen.data

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.options.AuthFlowType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.junit.Test

class AuthConfigurationTest {
    val jsonObject = JSONObject(
        """
            {
                "UserAgent": "aws-amplify-cli/0.1.0",
                "Version": "0.1.0",
                "IdentityManager": {
                    "Default": {}
                },
                "CredentialsProvider": {
                    "CognitoIdentity": {
                        "Default": {
                            "PoolId": "identity-pool-id",
                            "Region": "us-east-1"
                        }
                    }
                },
                "CognitoUserPool": {
                    "Default": {
                        "PoolId": "user-pool-id",
                        "AppClientId": "user-app-id",
                        "Region": "us-east-1"
                    }
                },
                "Auth": {
                    "Default": {
                        "authenticationFlowType": "USER_SRP_AUTH",
                        "OAuth": {
                          "AppClientId": "testAppClientId",
                          "AppClientSecret": "testAppClientSecret",
                          "WebDomain": "webDomain",
                          "Scopes": ["oauth"],
                          "SignInRedirectURI": "http://example.com/signin",
                          "SignOutRedirectURI": "http://example.com/signout"
                        },
                        "socialProviders": [],
                        "usernameAttributes": ["USERNAME", "PHONE_NUMBER"],
                        "signupAttributes": [
                            "EMAIL", "NAME", "BIRTHDATE"
                        ],
                        "passwordProtectionSettings": {
                            "passwordPolicyMinLength": 10,
                            "passwordPolicyCharacters": ["REQUIRES_NUMBERS", "REQUIRES_LOWER"]
                        },
                        "mfaConfiguration": "OFF",
                        "mfaTypes": [
                            "SMS"
                        ],
                        "verificationMechanisms": [
                            "PHONE_NUMBER", "EMAIL"
                        ]
                    }
                }
            }
        """.trimIndent()
    )

    @Test
    fun `parses auth configuration`() {
        val configuration = AuthConfiguration.fromJson(jsonObject)

        configuration.authFlowType shouldBe AuthFlowType.USER_SRP_AUTH
        configuration.usernameAttributes shouldContainExactly listOf(
            UsernameAttribute.Username,
            UsernameAttribute.PhoneNumber
        )
        configuration.signUpAttributes shouldContainExactly listOf(
            AuthUserAttributeKey.email(),
            AuthUserAttributeKey.name(),
            AuthUserAttributeKey.birthdate()
        )
        configuration.passwordProtectionSettings?.shouldNotBeNull()
        configuration.passwordProtectionSettings?.run {
            length shouldBe 10
            requiresUpper.shouldBeFalse()
            requiresLower.shouldBeTrue()
            requiresSpecial.shouldBeFalse()
            requiresNumber.shouldBeTrue()
        }
        configuration.verificationMechanisms shouldContainExactly listOf(
            VerificationMechanism.PhoneNumber,
            VerificationMechanism.Email
        )
    }

    @Test
    fun `signupAttributes is empty if json field is missing`() {
        getAuthConfig().remove("signupAttributes")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.signUpAttributes.shouldBeEmpty()
    }

    @Test
    fun `usernameAttributes is empty if json field is missing`() {
        getAuthConfig().remove("usernameAttributes")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.usernameAttributes.shouldBeEmpty()
    }

    @Test
    fun `verificationMechanisms is empty if json field is missing`() {
        getAuthConfig().remove("verificationMechanisms")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.verificationMechanisms.shouldBeEmpty()
    }

    @Test
    fun `passwordProtectionSettings is null if json field is missing`() {
        getAuthConfig().remove("passwordProtectionSettings")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.passwordProtectionSettings?.shouldBeNull()
    }

    @Test
    fun `password min length defaults to zero if json field is missing`() {
        getPasswordSettings().remove("passwordPolicyMinLength")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.passwordProtectionSettings?.length shouldBe 0
    }

    @Test
    fun `password character requirements are false if json field is missing`() {
        getPasswordSettings().remove("passwordPolicyCharacters")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.passwordProtectionSettings?.requiresLower?.shouldBeFalse()
        configuration.passwordProtectionSettings?.requiresUpper?.shouldBeFalse()
        configuration.passwordProtectionSettings?.requiresNumber?.shouldBeFalse()
        configuration.passwordProtectionSettings?.requiresSpecial?.shouldBeFalse()
    }

    private fun getAuthConfig() = jsonObject.getJSONObject("Auth").getJSONObject("Default")
    private fun getPasswordSettings() = jsonObject.getJSONObject("Auth").getJSONObject("Default")
        .getJSONObject("passwordProtectionSettings")
}
