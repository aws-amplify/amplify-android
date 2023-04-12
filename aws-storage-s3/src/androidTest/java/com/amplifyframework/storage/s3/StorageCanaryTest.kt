package com.amplifyframework.storage.s3

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.operation.StorageUploadFileOperation
import com.amplifyframework.storage.options.StorageUploadFileOptions
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class StorageCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private const val TEMP_DIR_PROPERTY = "java.io.tmpdir"
        private val TEMP_DIR = System.getProperty(TEMP_DIR_PROPERTY)

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSS3StoragePlugin())
                Amplify.addPlugin(AndroidLoggingPlugin(LogLevel.VERBOSE))
                Amplify.configure(ApplicationProvider.getApplicationContext())
                Log.i("StorageCanaryTest", "Initialized Amplify")
            } catch (error: AmplifyException) {
                Log.e("StorageCanaryTest", "Could not initialize Amplify", error)
            }
        }
    }

    @Test
    fun uploadInputStream() {
        val latch = CountDownLatch(1)
        val raf = createFile(1)
        val stream = FileInputStream(raf)
        try {
            Amplify.Storage.uploadInputStream(
                "ExampleKey", stream,
                {
                    Log.i("StorageCanaryTest", "Successfully uploaded: ${it.key}")
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "Upload failed", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun uploadFile() {
        val latch = CountDownLatch(1)
        val file = createFile(1)
        try {
            Amplify.Storage.uploadFile(
                "ExampleKey", file,
                {
                    Log.i("StorageCanaryTest", "Successfully uploaded: ${it.key}")
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "Upload failed", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun downloadFile() {
        val latch = CountDownLatch(1)
        val file = createFile(1)
        val fileName = "ExampleKey${UUID.randomUUID()}"
        Amplify.Storage.uploadFile(
            fileName, file,
            { Log.i("StorageCanaryTest", "Successfully uploaded: ${it.key}") },
            { Log.e("StorageCanaryTest", "Upload failed", it) }
        )
        try {
            Amplify.Storage.downloadFile(
                fileName, file,
                {
                    Log.i("StorageCanaryTest", "Successfully downloaded: ${it.file.name}")
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "Download Failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun getUrl() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Storage.getUrl(
                "ExampleKey",
                {
                    Log.i("StorageCanaryTest", "Successfully generated: ${it.url}")
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "URL generation failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun getTransfer() {
        val uploadLatch = CountDownLatch(1)
        val latch = CountDownLatch(1)
        val file = createFile(100)
        val fileName = "ExampleKey${UUID.randomUUID()}"
        val transferId = AtomicReference<String>()
        val opContainer = AtomicReference<StorageUploadFileOperation<*>>()
        val op = Amplify.Storage.uploadFile(
            fileName, file,
            StorageUploadFileOptions.builder().accessLevel(StorageAccessLevel.PUBLIC).build(),
            { progress ->
                if (progress.currentBytes > 0) {
                    opContainer.get().pause()
                }
                uploadLatch.countDown()
            },
            { Log.i("StorageCanaryTest", "Successfully uploaded: ${it.key}") },
            { Log.e("StorageCanaryTest", "Upload failed", it) }
        )
        opContainer.set(op)
        transferId.set(op.transferId)
        uploadLatch.await(TIMEOUT_S, TimeUnit.SECONDS)

        try {
            Amplify.Storage.getTransfer(
                transferId.get(),
                { operation ->
                    Log.i("StorageCanaryTest", "Current State" + operation.transferState)
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "Failed to query transfer", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun list() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Storage.list(
                "",
                { result ->
                    result.items.forEach { item ->
                        Log.i("StorageCanaryTest", "Item: ${item.key}")
                    }
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "List failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun remove() {
        val latch = CountDownLatch(1)
        try {
            Amplify.Storage.remove(
                "myUploadedFileName.txt",
                {
                    Log.i("StorageCanaryTest", "Successfully removed: ${it.key}")
                    latch.countDown()
                },
                {
                    Log.e("StorageCanaryTest", "Remove failure", it)
                    fail()
                }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    private fun createFile(size: Int): File {
        val file = File(TEMP_DIR + File.separator + "file")
        file.createNewFile()
        val raf = RandomAccessFile(file, "rw")
        raf.setLength((size * 1024 * 1024).toLong())
        raf.close()
        file.deleteOnExit()
        return file
    }
}
