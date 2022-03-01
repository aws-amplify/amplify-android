package com.amplifyframework.auth.cognito

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AuthConfiguration
import com.amplifyframework.auth.cognito.testutils.AuthConfigurationProvider
import com.amplifyframework.auth.cognito.testutils.CredentialStoreUtil
import com.google.gson.Gson
import junit.framework.Assert.assertTrue
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialStoreStateMachineInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val configuration = AuthConfigurationProvider.getAuthConfigurationObject()
    private val identityPoolId = configuration.credentials.cognitoIdentity.identityData.PoolId
    private val userPoolAppClientId = configuration.userPool.userPool.AppClientId

    private val credential = CredentialStoreUtil.getDefaultCredential()


    @Before
    fun setup() {
        CredentialStoreUtil.setupLegacyStore(context, userPoolAppClientId, identityPoolId)
    }

    private val authConfigJson = JSONObject(Gson().toJson(configuration))

    @Test
    fun test_CredentialStore_Migration_Succeeds_On_Plugin_Configuration() {
        AWSCognitoAuthPlugin().configure(authConfigJson, context)

        val credentialStore = AWSCognitoAuthCredentialStore(context, AuthConfiguration.fromJson(authConfigJson).build())
        val creds = credentialStore.retrieveCredential()

        assertTrue(creds == credential)
    }
}
