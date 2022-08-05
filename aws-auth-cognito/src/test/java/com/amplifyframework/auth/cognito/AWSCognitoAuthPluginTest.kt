/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

<<<<<<< Updated upstream
import android.app.Activity
import android.content.Intent
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
=======
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.AuthCategoryConfiguration
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.KeyValueRepository
import com.amplifyframework.auth.cognito.data.KeyValueRepositoryFactory
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import org.json.JSONException
import org.json.JSONObject
>>>>>>> Stashed changes
import org.junit.Before
import org.junit.Test
<<<<<<< Updated upstream
=======
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.util.*
>>>>>>> Stashed changes

class AWSCognitoAuthPluginTest {

<<<<<<< Updated upstream
    private lateinit var authPlugin: AWSCognitoAuthPlugin
    private val realPlugin: RealAWSCognitoAuthPlugin = mockk(relaxed = true)
=======
    private val PLUGIN_KEY = "awsCognitoAuthPlugin"

    private lateinit var authCategory: AuthCategory
    companion object {
        private const val IDENTITY_POOL_ID: String = "identityPoolID"
        private const val USER_POOL_ID: String = "userPoolID"
        private const val KEY_WITH_IDENTITY_POOL: String = "amplify.$IDENTITY_POOL_ID.session"
        private const val KEY_WITH_USER_POOL: String = "amplify.$USER_POOL_ID.session"
        private const val KEY_WITH_USER_AND_IDENTITY_POOL: String = "amplify.$USER_POOL_ID.$IDENTITY_POOL_ID.session"
    }

    private val keyValueRepoID: String = "com.amplify.credentialStore"

    @Mock
    private lateinit var mockConfig: AuthConfiguration

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockKeyValue: KeyValueRepository

    @Mock
    private lateinit var mockFactory: KeyValueRepositoryFactory

    private lateinit var persistentStore: AWSCognitoAuthCredentialStore

>>>>>>> Stashed changes

    @Before
    fun setup() {
        authPlugin = AWSCognitoAuthPlugin()
        authPlugin.realPlugin = realPlugin
    }

    @Test
    fun verifySignUp() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOptions = AuthSignUpOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signUp(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.signUp(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifyConfirmSignUp() {
        val expectedUsername = "user1"
        val expectedConfirmationCode = "aaab"
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignUp(expectedUsername, expectedConfirmationCode, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.confirmSignUp(
                expectedUsername,
                expectedConfirmationCode,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyOverloadedConfirmSignUp() {
        val expectedUsername = "user1"
        val expectedConfirmationCode = "aaab"
        val expectedOptions = AuthConfirmSignUpOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignUp(
            expectedUsername,
            expectedConfirmationCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify {
            realPlugin.confirmSignUp(
                expectedUsername,
                expectedConfirmationCode,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendSignUpCode(expectedUsername, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resendSignUpCode(expectedUsername, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedResendSignUpCode() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResendSignUpCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignUpResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendSignUpCode(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resendSignUpCode(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifySignIn() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signIn(expectedUsername, expectedPassword, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signIn(expectedUsername, expectedPassword, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedSignIn() {
        val expectedUsername = "user1"
        val expectedPassword = "abc123"
        val expectedOptions = AuthSignInOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signIn(expectedUsername, expectedPassword, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.signIn(
                expectedUsername,
                expectedPassword,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyConfirmSignIn() {
        val expectedConfirmationCode = "aaab"
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignIn(expectedConfirmationCode, expectedOnSuccess, expectedOnError)

        verify { realPlugin.confirmSignIn(expectedConfirmationCode, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedConfirmSignIn() {
        val expectedConfirmationCode = "aaab"
        val expectedOptions = AuthConfirmSignInOptions.defaults()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmSignIn(expectedConfirmationCode, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.confirmSignIn(expectedConfirmationCode, expectedOptions, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifySignInWithSocialWebUI() {
        val expectedProvider = AuthProvider.amazon()
        val expectedActivity: Activity = mockk()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithSocialWebUI(expectedProvider, expectedActivity, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.signInWithSocialWebUI(
                expectedProvider,
                expectedActivity,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyOverloadedSignInWithSocialWebUI() {
        val expectedProvider = AuthProvider.amazon()
        val expectedActivity: Activity = mockk()
        val expectedOptions = AuthWebUISignInOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithSocialWebUI(
            expectedProvider,
            expectedActivity,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify {
            realPlugin.signInWithSocialWebUI(
                expectedProvider,
                expectedActivity,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifySignInWithWebUI() {
        val expectedActivity: Activity = mockk()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithWebUI(expectedActivity, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signInWithWebUI(expectedActivity, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedSignInWithWebUI() {
        val expectedActivity: Activity = mockk()
        val expectedOptions = AuthWebUISignInOptions.builder().build()
        val expectedOnSuccess = Consumer<AuthSignInResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signInWithWebUI(expectedActivity, expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signInWithWebUI(expectedActivity, expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyHandleWebUISignInResponse() {
        val expectedIntent: Intent = mockk()

        authPlugin.handleWebUISignInResponse(expectedIntent)

        verify { realPlugin.handleWebUISignInResponse(expectedIntent) }
    }

    @Test
    fun verifyFetchAuthSession() {
        val expectedOnSuccess = Consumer<AuthSession> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchAuthSession(expectedOnSuccess, expectedOnError)

        verify { realPlugin.fetchAuthSession(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyRememberDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.rememberDevice(expectedOnSuccess, expectedOnError)

        verify { realPlugin.rememberDevice(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyForgetDevice() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.forgetDevice(expectedOnSuccess, expectedOnError)

        verify { realPlugin.forgetDevice(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedForgetDevice() {
        val expectedDevice = AuthDevice.fromId("id2")
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.forgetDevice(expectedDevice, expectedOnSuccess, expectedOnError)

        verify { realPlugin.forgetDevice(expectedDevice, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyFetchDevices() {
        val expectedOnSuccess = Consumer<MutableList<AuthDevice>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchDevices(expectedOnSuccess, expectedOnError)

        verify { realPlugin.fetchDevices(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyResetPassword() {
        val expectedUsername = "user1"
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resetPassword(expectedUsername, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resetPassword(expectedUsername, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedResetPassword() {
        val expectedUsername = "user1"
        val expectedOptions = AuthResetPasswordOptions.defaults()
        val expectedOnSuccess = Consumer<AuthResetPasswordResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resetPassword(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.resetPassword(expectedUsername, expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyConfirmResetPassword() {
        val expectedUsername = "user1"
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmResetPassword(
            expectedUsername,
            expectedPassword,
            expectedCode,
            expectedOnSuccess,
            expectedOnError
        )

        verify {
            realPlugin.confirmResetPassword(
                expectedUsername,
                expectedPassword,
                expectedCode,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyOverloadedConfirmResetPassword() {
        val expectedUsername = "user1"
        val expectedPassword = "p1234"
        val expectedCode = "4723j"
        val expectedOptions = AuthConfirmResetPasswordOptions.defaults()
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmResetPassword(
            expectedUsername,
            expectedPassword,
            expectedCode,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

        verify {
            realPlugin.confirmResetPassword(
                expectedUsername,
                expectedPassword,
                expectedCode,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyUpdatePassword() {
        val expectedOldPassword = "aldfkj1"
        val expectedNewPassword = "34w3ed"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updatePassword(expectedOldPassword, expectedNewPassword, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.updatePassword(expectedOldPassword, expectedNewPassword, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifyFetchUserAttributes() {
        val expectedOnSuccess = Consumer<MutableList<AuthUserAttribute>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.fetchUserAttributes(expectedOnSuccess, expectedOnError)

        verify { realPlugin.fetchUserAttributes(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttribute(expectedAttribute, expectedOnSuccess, expectedOnError)

        verify { realPlugin.updateUserAttribute(expectedAttribute, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttribute() {
        val expectedAttribute = AuthUserAttribute(AuthUserAttributeKey.name(), "John")
        val expectedOptions = AuthUpdateUserAttributeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthUpdateAttributeResult> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttribute(expectedAttribute, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.updateUserAttribute(expectedAttribute, expectedOptions, expectedOnSuccess, expectedOnError)
        }
    }

    @Test
    fun verifyUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOnSuccess = Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttributes(expectedAttributes, expectedOnSuccess, expectedOnError)

        verify { realPlugin.updateUserAttributes(expectedAttributes, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedUpdateUserAttributes() {
        val expectedAttributes = mutableListOf(AuthUserAttribute(AuthUserAttributeKey.name(), "John"))
        val expectedOptions = AuthUpdateUserAttributesOptions.defaults()
        val expectedOnSuccess = Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.updateUserAttributes(expectedAttributes, expectedOptions, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.updateUserAttributes(
                expectedAttributes,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyEscapeHatch() {
        val expectedEscapeHatch = mockk<AWSCognitoAuthServiceBehavior>()
        every { realPlugin.escapeHatch() } returns expectedEscapeHatch

        assertEquals(expectedEscapeHatch, authPlugin.escapeHatch)
    }

    @Test
    fun verifyResendUserAttributeConfirmationCode() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendUserAttributeConfirmationCode(expectedAttributeKey, expectedOnSuccess, expectedOnError)

        verify {
            realPlugin.resendUserAttributeConfirmationCode(
                expectedAttributeKey,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyOverloadedResendUserAttributeConfirmationCode() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedOptions = AuthResendUserAttributeConfirmationCodeOptions.defaults()
        val expectedOnSuccess = Consumer<AuthCodeDeliveryDetails> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.resendUserAttributeConfirmationCode(
            expectedAttributeKey,
            expectedOptions,
            expectedOnSuccess,
            expectedOnError
        )

<<<<<<< Updated upstream
        verify {
            realPlugin.resendUserAttributeConfirmationCode(
                expectedAttributeKey,
                expectedOptions,
                expectedOnSuccess,
                expectedOnError
            )
        }
=======
        val authConfig = AuthCategoryConfiguration()
        authConfig.populateFromJSON(json)
        authCategory.configure(authConfig, context)
        authCategory.initialize(context)

        Mockito.`when`(
            mockFactory.create(
                mockContext,
                keyValueRepoID,
                true,
            )
        ).thenReturn(mockKeyValue)

        Mockito.`when`(mockKeyValue.get(Mockito.anyString())).thenReturn(
            serialized(getCredential())
        )

        val provider = object : Provider("AndroidKeyStore", 1.0, "") {
            init {
                put("KeyStore.AndroidKeyStore",
                    FakeKeyStore::class.java.name)
            }
        }
        provider.pu
        Security.addProvider(provider)
    }

    private fun getCredential(): AmplifyCredential {
        val expiration = 123123L
        return AmplifyCredential(
            CognitoUserPoolTokens(
                "idToken",
                "accessToken",
                "refreshToken",
                expiration
            ),
            "identityPool",
            AWSCredentials(
                "accessKeyId",
                "secretAccessKey",
                "sessionToken",
                expiration
            )
        )
>>>>>>> Stashed changes
    }

    @Test
    fun verifyConfirmUserAttribute() {
        val expectedAttributeKey = AuthUserAttributeKey.name()
        val expectedConfirmationCode = "akj34"
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.confirmUserAttribute(
            expectedAttributeKey,
            expectedConfirmationCode,
            expectedOnSuccess,
            expectedOnError
        )

        verify {
            realPlugin.confirmUserAttribute(
                expectedAttributeKey,
                expectedConfirmationCode,
                expectedOnSuccess,
                expectedOnError
            )
        }
    }

    @Test
    fun verifyGetCurrentUser() {
        val expectedOnSuccess = Consumer<AuthUser> { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.getCurrentUser(expectedOnSuccess, expectedOnError)

        verify { realPlugin.getCurrentUser(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifySignOut() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signOut(expectedOnSuccess, expectedOnError)

        verify { realPlugin.signOut(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyOverloadedSignOut() {
        val expectedOnSuccess = Action { }
        val expectedOptions = AuthSignOutOptions.builder().build()
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.signOut(expectedOptions, expectedOnSuccess, expectedOnError)

        verify { realPlugin.signOut(expectedOptions, expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyDeleteUser() {
        val expectedOnSuccess = Action { }
        val expectedOnError = Consumer<AuthException> { }

        authPlugin.deleteUser(expectedOnSuccess, expectedOnError)

        verify { realPlugin.deleteUser(expectedOnSuccess, expectedOnError) }
    }

    @Test
    fun verifyPluginKey() {
        assertEquals("awsCognitoAuthPlugin", authPlugin.pluginKey)
    }

    @Test
    fun testRememberDevice() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
        val testLatch = CountDownLatch(1)
        authCategory.rememberDevice(
            { testLatch.countDown() },
            {}
        )
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    private fun serialized(credential: AmplifyCredential): String {
        return Json.encodeToString(credential)
    }

    private fun setStoreCredentials(credential: AmplifyCredential) {
        Mockito.`when`(mockKeyValue.get(Mockito.anyString())).thenReturn(serialized(credential))

        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
    }

    private fun setupIdentityPoolConfig() {
        Mockito.`when`(mockConfig.identityPool).thenReturn(
            IdentityPoolConfiguration {
                this.poolId = IDENTITY_POOL_ID
            }
        )
    }

    private fun setupUserPoolConfig() {
        Mockito.`when`(mockConfig.userPool).thenReturn(
            UserPoolConfiguration {
                this.poolId = USER_POOL_ID
                this.appClientId = ""
            }
        )
    }

    class FakeKeyStore : KeyStoreSpi() {
        private val wrapped =
            KeyStore.getInstance(KeyStore.getDefaultType())

        override fun engineIsKeyEntry(alias: String?) =
            wrapped.isKeyEntry(alias)
        override fun engineIsCertificateEntry(alias: String?) =
            wrapped.isCertificateEntry(alias)

        override fun engineGetCertificateAlias(p0: Certificate?): String {
            TODO("Not yet implemented")
        }

        override fun engineStore(p0: OutputStream?, p1: CharArray?) {
            TODO("Not yet implemented")
        }

        override fun engineLoad(p0: InputStream?, p1: CharArray?) {
            TODO("Not yet implemented")
        }

        override fun engineGetKey(alias: String?, password: CharArray?)=
            wrapped.getKey(alias, password)

        override fun engineGetCertificateChain(p0: String?): Array<Certificate> {
            TODO("Not yet implemented")
        }

        override fun engineGetCertificate(p0: String?): Certificate {
            TODO("Not yet implemented")
        }

        override fun engineGetCreationDate(p0: String?): Date {
            TODO("Not yet implemented")
        }

        override fun engineSetKeyEntry(p0: String?, p1: Key?, p2: CharArray?, p3: Array<out Certificate>?) {
            TODO("Not yet implemented")
        }

        override fun engineSetKeyEntry(p0: String?, p1: ByteArray?, p2: Array<out Certificate>?) {
            TODO("Not yet implemented")
        }

        override fun engineSetCertificateEntry(p0: String?, p1: Certificate?) {
            TODO("Not yet implemented")
        }

        override fun engineDeleteEntry(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun engineAliases(): Enumeration<String> {
            TODO("Not yet implemented")
        }

        override fun engineContainsAlias(p0: String?): Boolean {
            TODO("Not yet implemented")
        }

        override fun engineSize(): Int {
            TODO("Not yet implemented")
        }
    }
}
