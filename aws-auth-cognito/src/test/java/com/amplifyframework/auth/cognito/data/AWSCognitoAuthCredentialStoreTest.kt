package com.amplifyframework.auth.cognito.data

import android.content.Context
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.statemachine.codegen.data.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@RunWith(MockitoJUnitRunner::class)
class AWSCognitoAuthCredentialStoreTest {

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

    @Before
    fun setup() {
        Mockito.`when`(mockFactory.create(
            mockContext,
            keyValueRepoID,
            true,
        )).thenReturn(mockKeyValue)

        Mockito.`when`(mockKeyValue.get(Mockito.anyString())).thenReturn(serialized(getCredential()))
    }

    @Test
    fun testSaveCredentialWithUserPool() {
        setupUserPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
        persistentStore.saveCredential(getCredential())
        verify(mockKeyValue, times(1))
            .put(KEY_WITH_USER_POOL, serialized(getCredential()))
    }

    @Test
    fun testSaveCredentialWithIdentityPool() {
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
        
        persistentStore.saveCredential(getCredential())
        
        verify(mockKeyValue, times(1))
            .put(KEY_WITH_IDENTITY_POOL, serialized(getCredential()))
    }

    @Test
    fun testSaveCredentialWithUserAndIdentityPool() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)

        persistentStore.saveCredential(getCredential())

        verify(mockKeyValue, times(1))
            .put(KEY_WITH_USER_AND_IDENTITY_POOL, serialized(getCredential()))
    }

    @Test
    fun testRetrieveCredential() {
        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
        
        val actual = persistentStore.retrieveCredential()
        
        Assert.assertEquals(actual, getCredential())
    }

    @Test
    fun testDeleteCredential() {
        setupUserPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
        
        persistentStore.deleteCredential()
        
        verify(mockKeyValue, times(1)).remove(KEY_WITH_USER_POOL)
    }

    @Test
    fun testInMemoryCredentialStore() {
        val store = AWSCognitoAuthCredentialStore(mockContext, mockConfig, false)

        store.saveCredential(getCredential())
        assertEquals(getCredential(), store.retrieveCredential())

        store.deleteCredential()
        assertEquals(null, store.retrieveCredential())
    }

    @Test
    fun testCognitoUserPoolTokensIsReturnedAsNullIfAllItsFieldsAreNull() {
        val credential = getCredential().copy(
            cognitoUserPoolTokens = CognitoUserPoolTokens(null, null, null, null)
        )
        setStoreCredentials(credential)

        val actual = persistentStore.retrieveCredential()?.cognitoUserPoolTokens

        Assert.assertEquals(null, actual)
    }

    @Test
    fun testAWSCredentialsIsReturnedAsNullIfAllItsFieldsAreNull() {
        val credential = getCredential().copy(
            awsCredentials = AWSCredentials(null, null, null, null)
        )
        setStoreCredentials(credential)

        val actual = persistentStore.retrieveCredential()?.awsCredentials

        Assert.assertEquals(null, actual)
    }

    private fun setStoreCredentials(credential: AmplifyCredential) {
        Mockito.`when`(mockKeyValue.get(Mockito.anyString())).thenReturn(serialized(credential))

        setupUserPoolConfig()
        setupIdentityPoolConfig()
        persistentStore = AWSCognitoAuthCredentialStore(mockContext, mockConfig, true, mockFactory)
    }

    private fun setupIdentityPoolConfig() {
        Mockito.`when`(mockConfig.identityPool).thenReturn(IdentityPoolConfiguration {
            this.poolId = IDENTITY_POOL_ID
        })
    }

    private fun setupUserPoolConfig() {
        Mockito.`when`(mockConfig.userPool).thenReturn(UserPoolConfiguration {
            this.poolId = USER_POOL_ID
            this.appClientId = ""
        })
    }

    private fun getCredential(): AmplifyCredential {
        return AmplifyCredential(
            CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", Instant.now().plus(123123.seconds).epochSeconds),
            "identityPool",
            AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", Instant.now().plus(123123.seconds).epochSeconds)
        )
    }

    private fun serialized(credential: AmplifyCredential): String {
        return Json.encodeToString(credential)
    }
}

