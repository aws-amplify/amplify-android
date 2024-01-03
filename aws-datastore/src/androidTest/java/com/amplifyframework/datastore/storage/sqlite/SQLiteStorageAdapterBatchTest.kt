package com.amplifyframework.datastore.storage.sqlite

import com.amplifyframework.core.Consumer
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.StrictMode
import com.amplifyframework.datastore.storage.StorageItemChange
import com.amplifyframework.datastore.storage.StorageOperation
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider
import com.amplifyframework.testmodels.commentsblog.BlogOwner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SQLiteStorageAdapterBatchTest {
    private lateinit var adapter: SynchronousStorageAdapter

    @Before
    fun setup() {
        StrictMode.enable()
        TestStorageAdapter.cleanup()
        adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance())
    }

    @After
    fun teardown() {
        TestStorageAdapter.cleanup(adapter)
    }

    @Test
    @Throws(DataStoreException::class)
    fun batchSyncWithMixedOperations() {
        // Write to update later
        val toUpdate = BlogOwner.builder()
            .name("Update")
            .id("UPDATE")
            .build()
        adapter.save(toUpdate)
        val updated = toUpdate.copyOfBuilder().name("Updated").build()
        val toDelete = BlogOwner.builder()
            .name("Delete")
            .id("DELETE")
            .build()
        adapter.save(toDelete)
        // Don't write toCreate yet
        val toCreate = BlogOwner.builder()
            .name("Create")
            .id("CREATE")
            .build()
        val capturedChanges = mutableListOf<StorageItemChange<BlogOwner>>()
        val captureConsumer = Consumer<StorageItemChange<BlogOwner>> {
            capturedChanges.add(it)
        }

        val operations = listOf(
            StorageOperation.Create(updated, captureConsumer),
            StorageOperation.Create(toCreate, captureConsumer),
            StorageOperation.Delete(toDelete, captureConsumer)

        )
        adapter.batchSyncOperations(operations)

        // Get the BlogOwner from the database
        val blogOwners = adapter.query(BlogOwner::class.java)
        assertEquals(2, blogOwners.size)
        assertNotNull(blogOwners.find { it == updated })
        assertNotNull(blogOwners.find { it == toCreate })
        assertNull(blogOwners.find { it == toDelete })
        assertEquals(3, capturedChanges.size)
        assertEquals(capturedChanges[0].item(), updated)
        assertEquals(capturedChanges[1].item(), toCreate)
        assertEquals(capturedChanges[2].item(), toDelete)
        assertEquals(capturedChanges[0].initiator(), StorageItemChange.Initiator.SYNC_ENGINE)
        assertEquals(capturedChanges[1].initiator(), StorageItemChange.Initiator.SYNC_ENGINE)
        assertEquals(capturedChanges[2].initiator(), StorageItemChange.Initiator.SYNC_ENGINE)
    }

    @Test
    @Throws(DataStoreException::class)
    fun deleteFailureDoesNotStopSync() {
        val toDelete = BlogOwner.builder()
            .name("Delete")
            .id("DELETE")
            .build()
        val toCreate = BlogOwner.builder()
            .name("Create")
            .id("CREATE")
            .build()
        val capturedChanges = mutableListOf<StorageItemChange<BlogOwner>>()
        val captureConsumer = Consumer<StorageItemChange<BlogOwner>> {
            capturedChanges.add(it)
        }
        val operations = listOf(
            StorageOperation.Delete(toDelete, captureConsumer),
            StorageOperation.Create(toCreate, captureConsumer)
        )

        adapter.batchSyncOperations(operations)

        // Get the BlogOwner from the database
        val blogOwners = adapter.query(BlogOwner::class.java)
        assertEquals(1, blogOwners.size)
        assertNotNull(blogOwners.find { it == toCreate })
        assertEquals(2, capturedChanges.size)
        assertEquals(capturedChanges[0].item(), toDelete)
        assertEquals(capturedChanges[1].item(), toCreate)
        assertEquals(capturedChanges[0].initiator(), StorageItemChange.Initiator.SYNC_ENGINE)
        assertEquals(capturedChanges[1].initiator(), StorageItemChange.Initiator.SYNC_ENGINE)
    }
}
