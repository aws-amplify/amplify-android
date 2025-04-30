package com.amplifyframework.core.store

import android.security.keystore.KeyProperties
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import java.util.UUID
import javax.crypto.KeyGenerator
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmplifyKeyValueRepositoryTest {

    companion object {
        private val testKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES).generateKey()
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    @After
    fun tearDown() {
        Security.removeProvider("AndroidKeyStore")
    }

    @Test
    fun `get encrypted repository succeeds`() {
        Security.addProvider(FakeAndroidKeyStoreProvider(GoodFakeKeyStore::class.java.name))
        val expectedPrefsName = UUID.randomUUID().toString()
        val expectedKey = "k1"
        val expectedValue = "v1"

        AmplifyKeyValueRepository(context, expectedPrefsName).apply {
            put(expectedKey, expectedValue)
        }

        EncryptedKeyValueRepository(context, expectedPrefsName).get(expectedKey) shouldBe expectedValue
    }

    @Test
    fun `get in memory repository fallback succeeds`() {
        Security.addProvider(FakeAndroidKeyStoreProvider(BadFakeKeyStore::class.java.name))
        val expectedPrefsName = UUID.randomUUID().toString()
        val expectedKey = "k1"
        val expectedValue = "v1"

        AmplifyKeyValueRepository(context, expectedPrefsName).apply {
            put(expectedKey, expectedValue)
        }

        InMemoryKeyValueRepositoryProvider
            .getKeyValueRepository(expectedPrefsName)
            .get(expectedKey) shouldBe expectedValue
    }

    @Test
    fun `test KeyValueRepository method passthrough`() {
        Security.addProvider(FakeAndroidKeyStoreProvider(BadFakeKeyStore::class.java.name))
        val expectedPrefsName = UUID.randomUUID().toString()
        val expectedKey = "k1"
        val expectedValue = "v1"
        val expectedKey2 = "k1"
        val expectedValue2 = "v1"

        val repository = AmplifyKeyValueRepository(context, expectedPrefsName)
        val inMemoryRepository = InMemoryKeyValueRepositoryProvider.getKeyValueRepository(expectedPrefsName)

        // validate put passes through to in memory repository
        repository.put(expectedKey, expectedValue)
        inMemoryRepository.get(expectedKey) shouldBe expectedValue

        // validate get reads from in memory repository
        inMemoryRepository.put(expectedKey, expectedValue)
        repository.get(expectedKey2) shouldBe expectedValue2

        // validate remove passes through to in memory repository
        repository.remove(expectedKey)
        inMemoryRepository.get(expectedKey) shouldBe null

        // validate removeAll passes through to in memory repository
        repository.removeAll()
        inMemoryRepository.get(expectedKey2) shouldBe null
    }

    class FakeAndroidKeyStoreProvider(androidKeyStoreClassName: String) : Provider(
        "AndroidKeyStore",
        1.0,
        "Fake AndroidKeyStore provider"
    ) {
        init {
            put("KeyStore.AndroidKeyStore", androidKeyStoreClassName)
        }
    }

    open class FakeKeyStore : KeyStoreSpi() {
        override fun engineIsKeyEntry(alias: String?) = true
        override fun engineIsCertificateEntry(alias: String?) = true
        override fun engineGetCertificate(alias: String?): Certificate = throw NotImplementedError()
        override fun engineGetCreationDate(alias: String?): Date = Date()
        override fun engineDeleteEntry(alias: String?) {}
        override fun engineSetKeyEntry(
            alias: String?,
            key: Key?,
            password: CharArray?,
            chain: Array<out Certificate>?
        ) {}
        override fun engineGetEntry(alias: String?, protParam: KeyStore.ProtectionParameter?): KeyStore.Entry =
            throw NotImplementedError()
        override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) {}
        override fun engineStore(stream: OutputStream?, password: CharArray?) {}
        override fun engineSize() = 1
        override fun engineAliases(): Enumeration<String> = Collections.emptyEnumeration()
        override fun engineContainsAlias(alias: String?) = true
        override fun engineLoad(stream: InputStream?, password: CharArray?) {}
        override fun engineGetCertificateChain(alias: String?) = emptyArray<Certificate>()
        override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) {}
        override fun engineGetCertificateAlias(cert: Certificate?) = null
        override fun engineGetKey(alias: String?, password: CharArray?): Key? =
            throw NotImplementedError("No Need to implement for testing")
    }

    class GoodFakeKeyStore : FakeKeyStore() {
        override fun engineGetKey(alias: String?, password: CharArray?): Key = testKey
    }

    class BadFakeKeyStore : FakeKeyStore() {
        override fun engineGetKey(alias: String?, password: CharArray?): Key? = null
    }
}
