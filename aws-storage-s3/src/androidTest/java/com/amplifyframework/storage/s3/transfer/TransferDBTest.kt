/**
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.storage.s3.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.storage.TransferState
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

open class TransferDBTest {
    private val bucketName = "bucket_name"
    private val fileKey = "file_key"
    private lateinit var transferDB: TransferDB
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        transferDB = TransferDB.getInstance(context)
        tempFile = File.createTempFile("tempFile", ".txt")
    }

    @After
    fun tearDown() {
        transferDB.closeDB()
        tempFile.delete()
    }

    @Test
    fun testInsertSingleTransferRecord() {
        val transferId = UUID.randomUUID().toString()
        val uri = transferDB.insertSingleTransferRecord(
            transferId,
            TransferType.UPLOAD,
            bucketName,
            fileKey,
            tempFile,
            null,
            null
        )

        assertOnTransferRecord(uri) {
            Assert.assertEquals(transferId, it.transferId)
            Assert.assertEquals(TransferType.UPLOAD, it.type)
            Assert.assertEquals(tempFile, File(it.file))
            Assert.assertEquals(fileKey, it.key)
            Assert.assertEquals(bucketName, it.bucketName)
        }
    }

    @Test
    fun testMultiPartUploadRecord() {
        val uploadID = UUID.randomUUID().toString()
        val uri = transferDB.insertMultipartUploadRecord(
            uploadID,
            bucketName,
            fileKey,
            tempFile,
            1L,
            1,
            uploadID,
            1L,
            1
        )

        assertOnTransferRecord(uri) {
            Assert.assertEquals(TransferType.UPLOAD, it.type)
            Assert.assertEquals(tempFile, File(it.file))
            Assert.assertEquals(fileKey, it.key)
            Assert.assertEquals(bucketName, it.bucketName)
            Assert.assertEquals(uploadID, it.multipartId)
        }
    }

    @Test
    fun testMultiPartDelete() {
        val key = UUID.randomUUID().toString()
        val contentValues = arrayOfNulls<ContentValues>(3)
        contentValues[0] = transferDB.generateContentValuesForMultiPartUpload(
            key,
            bucketName,
            key,
            tempFile,
            0L,
            0,
            null,
            1L,
            0,
            null,
            null
        )
        contentValues[1] = transferDB.generateContentValuesForMultiPartUpload(
            key,
            bucketName,
            key,
            tempFile,
            0L,
            1,
            null,
            1L,
            0,
            null,
            null
        )
        contentValues[2] = transferDB.generateContentValuesForMultiPartUpload(
            key,
            bucketName,
            key,
            tempFile,
            0L,
            2,
            null,
            1L,
            1,
            null,
            null
        )
        val bulkInsertUri = transferDB.bulkInsertTransferRecords(contentValues)
        transferDB.getTransferRecordById(bulkInsertUri)
        val result = transferDB.deletePartTransferRecords(bulkInsertUri)
        Assert.assertEquals(result, 2)
    }

    @Test
    fun testUpdateBytesTransferred() {
        val transferId = UUID.randomUUID().toString()
        val uri = transferDB.insertSingleTransferRecord(
            transferId,
            TransferType.UPLOAD,
            bucketName,
            fileKey,
            tempFile,
            null,
            null
        )
        uri.lastPathSegment?.let { id ->
            transferDB.updateBytesTransferred(id.toInt(), 100L, 1000L)
        }

        assertOnTransferRecord(uri) {
            Assert.assertEquals(100L, it.bytesCurrent)
            Assert.assertEquals(1000L, it.bytesTotal)
        }
    }

    @Test
    fun testUpdateWorkManagerRequestId() {
        val transferId = UUID.randomUUID().toString()
        val uri = transferDB.insertSingleTransferRecord(
            transferId,
            TransferType.UPLOAD,
            bucketName,
            fileKey,
            tempFile,
            null,
            null
        )
        val workManagerRequestId = UUID.randomUUID().toString()
        uri.lastPathSegment?.let { id ->
            transferDB.updateWorkManagerRequestId(id.toInt(), workManagerRequestId)
        }

        assertOnTransferRecord(uri) {
            Assert.assertEquals(100L, it.bytesCurrent)
            Assert.assertEquals(1000L, it.bytesTotal)
        }
    }

    @Test
    fun testQueryBytesTransferredByMainUploadId() {
        val key = UUID.randomUUID().toString()
        val contentValues = arrayOfNulls<ContentValues>(3)
        contentValues[0] = transferDB.generateContentValuesForMultiPartUpload(
            key,
            bucketName,
            key,
            tempFile,
            0L,
            0,
            null,
            200L,
            0,
            null,
            null
        )
        contentValues[1] = transferDB.generateContentValuesForMultiPartUpload(
            key,
            bucketName,
            key,
            tempFile,
            0L,
            1,
            null,
            100L,
            0,
            null,
            null
        )
        contentValues[2] = transferDB.generateContentValuesForMultiPartUpload(
            key,
            bucketName,
            key,
            tempFile,
            0L,
            2,
            null,
            100L,
            1,
            null,
            null
        )
        val bulkInsertUri = transferDB.bulkInsertTransferRecords(contentValues)

        val parts = transferDB.getNonCompletedPartRequestsFromDB(bulkInsertUri)
        // mark first part as completed
        transferDB.updateState(parts[0], TransferState.PART_COMPLETED)
        Assert.assertEquals(100L, transferDB.queryBytesTransferredByMainUploadId(bulkInsertUri))
    }

    private fun assertOnTransferRecord(uri: Uri, block: (TransferRecord) -> Unit) {
        getInsertedRecord(uri)?.run {
            block
        } ?: Assert.fail("InsertedRecord is null")
    }

    private fun getInsertedRecord(uri: Uri): TransferRecord? {
        return transferDB.getTransferRecordById(uri.lastPathSegment?.toInt() ?: 0)
    }
}
