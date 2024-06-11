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
import com.amplifyframework.storage.ObjectMetadata
import java.io.File
import java.sql.Date
import java.time.Instant
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

        getInsertedRecord(uri)?.run {
            Assert.assertEquals(transferId, this.transferId)
            Assert.assertEquals(TransferType.UPLOAD, this.type)
            Assert.assertEquals(tempFile, File(this.file))
            Assert.assertEquals(fileKey, this.key)
            Assert.assertEquals(bucketName, this.bucketName)
        } ?: Assert.fail("InsertedRecord is null")
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

        getInsertedRecord(uri)?.run {
            Assert.assertEquals(TransferType.UPLOAD, this.type)
            Assert.assertEquals(tempFile, File(this.file))
            Assert.assertEquals(fileKey, this.key)
            Assert.assertEquals(bucketName, this.bucketName)
            Assert.assertEquals(uploadID, this.multipartId)
        } ?: Assert.fail("InsertedRecord is null")
    }

    @Test
    fun generateContentValuesForMultiPartUploadWithMetadata() {
        val key = UUID.randomUUID().toString()
        val expectedHttpExpiresDate = Date.from(Instant.now())
        val expectedExpirationDate = Date.from(Instant.EPOCH)
        val restoreExpirationTime = Date.from(Instant.EPOCH)
        val contentValues = arrayOfNulls<ContentValues>(1)
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
            ObjectMetadata(
                userMetadata = mapOf("key1" to "value1"),
                metaData = mutableMapOf("key1" to "value1"),
                httpExpiresDate = expectedHttpExpiresDate,
                expirationTime = expectedExpirationDate,
                expirationTimeRuleId = "ruleId",
                ongoingRestore = false,
                restoreExpirationTime = restoreExpirationTime
            ),
            null,
            useAccelerateEndpoint = false
        )
        val uri = transferDB.bulkInsertTransferRecords(contentValues)
        transferDB.getTransferRecordById(uri).run {
            Assert.assertEquals(mapOf("key1" to "value1"), this?.userMetadata)
            Assert.assertNull(this?.headerStorageClass)
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
            null,
            false
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
            null,
            false
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
            null,
            false
        )
        val bulkInsertUri = transferDB.bulkInsertTransferRecords(contentValues)
        transferDB.getTransferRecordById(bulkInsertUri)
        val result = transferDB.deletePartTransferRecords(bulkInsertUri)
        Assert.assertEquals(result, 2)
    }

    private fun getInsertedRecord(uri: Uri): TransferRecord? {
        val queryResult = transferDB.queryTransferById(uri.lastPathSegment?.toInt() ?: 0)
        var resultRecord: TransferRecord? = null
        queryResult?.let {
            while (it.moveToNext()) {
                resultRecord = TransferRecord.updateFromDB(it)
            }
        }
        return resultRecord
    }
}
