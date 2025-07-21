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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.EmailMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SetUserMfaPreferenceRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SetUserMfaPreferenceResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SmsMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaSettingsType
import com.amplifyframework.auth.MFAType.EMAIL
import com.amplifyframework.auth.MFAType.SMS
import com.amplifyframework.auth.MFAType.TOTP
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.MFAPreference
import com.amplifyframework.auth.cognito.MFAPreference.DISABLED
import com.amplifyframework.auth.cognito.MFAPreference.ENABLED
import com.amplifyframework.auth.cognito.MFAPreference.NOT_PREFERRED
import com.amplifyframework.auth.cognito.MFAPreference.PREFERRED
import com.amplifyframework.auth.cognito.UserMFAPreference
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UpdateMfaPreferenceUseCaseParameterizedTest(private val testCase: TestCase) {

    private val client: CognitoIdentityProviderClient = mockk {
        coEvery { setUserMfaPreference(any()) } returns SetUserMfaPreferenceResponse {}
    }
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val fetchMFAPreference: FetchMfaPreferenceUseCase = mockk {
        coEvery { execute() } returns UserMFAPreference(
            enabled = emptySet(),
            preferred = null
        )
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }

    private val useCase = UpdateMfaPreferenceUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        fetchMfaPreference = fetchMFAPreference,
        stateMachine = stateMachine
    )

    @Test
    fun `executes expected request`() = runTest {
        coEvery { fetchMFAPreference.execute() } returns testCase.current

        useCase.execute(
            sms = testCase.args.sms,
            totp = testCase.args.totp,
            email = testCase.args.email
        )

        coVerify {
            client.setUserMfaPreference(testCase.expected)
        }
    }

    companion object {
        data class UseCaseArgs(
            val sms: MFAPreference? = null,
            val totp: MFAPreference? = null,
            val email: MFAPreference? = null
        )

        data class TestCase(
            val name: String,
            val current: UserMFAPreference,
            val args: UseCaseArgs,
            val expected: SetUserMfaPreferenceRequest
        ) {
            override fun toString() = name
        }

        private fun request(func: ExpectedRequestBuilder.() -> Unit) = ExpectedRequestBuilder().apply(func).build()

        private class ExpectedRequestBuilder {
            private var sms: SmsMfaSettingsType? = null
            private var totp: SoftwareTokenMfaSettingsType? = null
            private var email: EmailMfaSettingsType? = null
            fun sms(enabled: Boolean, preferred: Boolean) {
                sms = SmsMfaSettingsType {
                    this.enabled = enabled
                    this.preferredMfa = preferred
                }
            }
            fun totp(enabled: Boolean, preferred: Boolean) {
                totp = SoftwareTokenMfaSettingsType {
                    this.enabled = enabled
                    this.preferredMfa = preferred
                }
            }
            fun email(enabled: Boolean, preferred: Boolean) {
                email = EmailMfaSettingsType {
                    this.enabled = enabled
                    this.preferredMfa = preferred
                }
            }
            fun build() = SetUserMfaPreferenceRequest {
                accessToken = "access token"
                sms?.let { smsMfaSettings = it }
                totp?.let { softwareTokenMfaSettings = it }
                email?.let { emailMfaSettings = it }
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<TestCase> = listOf(
            TestCase(
                name = "totp stays preferred if marked enabled",
                current = UserMFAPreference(enabled = setOf(SMS, TOTP), preferred = TOTP),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = true)
                }
            ),
            TestCase(
                name = "totp can be marked not-preferred",
                current = UserMFAPreference(enabled = setOf(SMS, TOTP), preferred = TOTP),
                args = UseCaseArgs(sms = DISABLED, totp = NOT_PREFERRED, email = ENABLED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "sms stays preferred if marked enabled",
                current = UserMFAPreference(enabled = setOf(SMS, TOTP), preferred = SMS),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = true)
                    totp(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "sms can be marked not-preferred",
                current = UserMFAPreference(enabled = setOf(SMS, TOTP), preferred = SMS),
                args = UseCaseArgs(sms = NOT_PREFERRED, totp = ENABLED, email = DISABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "email stays preferred if marked enabled",
                current = UserMFAPreference(enabled = setOf(SMS, TOTP, EMAIL), preferred = EMAIL),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = true)
                }
            ),
            TestCase(
                name = "email can be marked not-preferred",
                current = UserMFAPreference(enabled = setOf(SMS, TOTP, EMAIL), preferred = EMAIL),
                args = UseCaseArgs(sms = DISABLED, totp = ENABLED, email = NOT_PREFERRED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "all types marked enabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "only sms marked enabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = ENABLED, totp = DISABLED, email = DISABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = false, preferred = false)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "only totp marked enabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = DISABLED, totp = ENABLED, email = DISABLED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "only email marked enabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = DISABLED, totp = DISABLED, email = ENABLED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = false, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "sms marked preferred",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = PREFERRED, totp = ENABLED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = true)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "totp marked preferred",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = ENABLED, totp = PREFERRED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = true)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "email marked preferred",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED, email = PREFERRED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = true)
                }
            ),
            TestCase(
                name = "totp preferred and others disabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = DISABLED, totp = PREFERRED, email = DISABLED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = true, preferred = true)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "sms preferred and others disabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = PREFERRED, totp = DISABLED, email = DISABLED),
                expected = request {
                    sms(enabled = true, preferred = true)
                    totp(enabled = false, preferred = false)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "email preferred and others disabled",
                current = UserMFAPreference(enabled = null, preferred = null),
                args = UseCaseArgs(sms = DISABLED, totp = DISABLED, email = PREFERRED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = false, preferred = false)
                    email(enabled = true, preferred = true)
                }
            ),
            TestCase(
                name = "totp can be disabled when preferred",
                current = UserMFAPreference(enabled = setOf(TOTP), preferred = TOTP),
                args = UseCaseArgs(sms = ENABLED, totp = DISABLED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = false, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "sms can be disabled",
                current = UserMFAPreference(enabled = setOf(SMS), preferred = SMS),
                args = UseCaseArgs(sms = DISABLED, totp = ENABLED, email = ENABLED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "email can be disabled",
                current = UserMFAPreference(enabled = setOf(EMAIL), preferred = EMAIL),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED, email = DISABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "sms and email can be disabled",
                current = UserMFAPreference(enabled = setOf(SMS), preferred = SMS),
                args = UseCaseArgs(sms = DISABLED, totp = ENABLED, email = DISABLED),
                expected = request {
                    sms(enabled = false, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = false, preferred = false)
                }
            ),
            TestCase(
                name = "sms can be marked preferred if totp is currently preferred",
                current = UserMFAPreference(enabled = setOf(TOTP), preferred = TOTP),
                args = UseCaseArgs(sms = PREFERRED, totp = ENABLED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = true)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "totp can be marked preferred if sms is currently preferred",
                current = UserMFAPreference(enabled = setOf(SMS), preferred = SMS),
                args = UseCaseArgs(sms = ENABLED, totp = PREFERRED, email = ENABLED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = true)
                    email(enabled = true, preferred = false)
                }
            ),
            TestCase(
                name = "email can be marked preferred if sms is currently preferred",
                current = UserMFAPreference(enabled = setOf(SMS), preferred = SMS),
                args = UseCaseArgs(sms = ENABLED, totp = ENABLED, email = PREFERRED),
                expected = request {
                    sms(enabled = true, preferred = false)
                    totp(enabled = true, preferred = false)
                    email(enabled = true, preferred = true)
                }
            )
        )
    }
}

// Non-parameterized tests
class UpdateMfaPreferenceUseCaseTest {
    private val client: CognitoIdentityProviderClient = mockk {
        coEvery { setUserMfaPreference(any()) } returns SetUserMfaPreferenceResponse {}
    }
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val fetchMFAPreference: FetchMfaPreferenceUseCase = mockk {
        coEvery { execute() } returns UserMFAPreference(
            enabled = emptySet(),
            preferred = null
        )
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }

    private val useCase = UpdateMfaPreferenceUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        fetchMfaPreference = fetchMFAPreference,
        stateMachine = stateMachine
    )

    @Test
    fun `fails if not in signed in state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute(ENABLED, ENABLED, PREFERRED)
        }
    }

    @Test
    fun `fails if all arguments are null`() = runTest {
        shouldThrow<InvalidParameterException> {
            useCase.execute(null, null, null)
        }
    }

    @Test
    fun `fails if unable to get access token`() = runTest {
        coEvery { fetchAuthSession.execute().accessToken } returns null

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute(ENABLED, ENABLED, PREFERRED)
        }
    }

    @Test
    fun `fails if unable to get current preferences`() = runTest {
        val exception = Exception()

        coEvery { fetchMFAPreference.execute() } throws exception

        shouldThrowAny {
            useCase.execute(ENABLED, ENABLED, ENABLED)
        } shouldBe exception
    }

    @Test
    fun `fails if client throws an error setting preferences`() = runTest {
        val exception = Exception()

        coEvery { client.setUserMfaPreference(any()) } throws exception

        shouldThrowAny {
            useCase.execute(ENABLED, ENABLED, ENABLED)
        } shouldBe exception
    }
}
