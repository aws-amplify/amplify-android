package com.amplifyframework.auth.cognito

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.auth.cognito.testutils.AuthConfigurationProvider
import com.amplifyframework.auth.cognito.testutils.CredentialStoreUtil
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AWSCognitoLegacyCredentialStoreInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val configuration: AuthConfiguration = AuthConfigurationProvider.getAuthConfiguration()

    private val credential = CredentialStoreUtil.getDefaultCredential()

    private lateinit var store: AWSCognitoLegacyCredentialStore

    @Before
    fun setup() {
        store = AWSCognitoLegacyCredentialStore(context, configuration)
        CredentialStoreUtil.setupLegacyStore(context, "appClientId", "identityPoolId")
    }

    @Test
    fun test_legacy_store_implementation_can_retrieve_credentials_stored_using_aws_sdk() {
        val creds = store.retrieveCredential()
        assertTrue(creds == credential)
    }
}
