package com.amplifyframework.storage.s3.service

import android.content.Context
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.presigners.presign
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.StorageItem
import com.amplifyframework.storage.s3.transfer.TransferManager
import com.amplifyframework.storage.s3.transfer.TransferObserver
import com.amplifyframework.storage.s3.transfer.UploadOptions
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking

/**
 * A representation of an S3 backend service endpoint.
 */
internal class AWSS3StorageService(
    private val context: Context,
    private val awsRegion: String,
    private val s3BucketName: String,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private val awsS3StoragePluginKey: String
) : StorageService {

    private var s3Client: S3Client = S3Client {
        region = awsRegion
        authCredentialsProvider
    }

    private val transferManager: TransferManager =
        TransferManager(context, s3Client, awsS3StoragePluginKey)

    /**
     * Generate pre-signed URL for an object.
     * @param serviceKey S3 service key
     * @param expires Number of seconds before URL expires
     * @return A pre-signed URL
     */
    @OptIn(ExperimentalTime::class)
    override fun getPresignedUrl(serviceKey: String, expires: Int): URL {
        val presignUrlRequest = runBlocking {
            GetObjectRequest {
                bucket = s3BucketName
                key = serviceKey
            }.presign(s3Client.config, expires.seconds)
        }
        return URL(presignUrlRequest.url.toString())
    }

    /**
     * Begin downloading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @return A transfer observer
     */
    override fun downloadToFile(serviceKey: String, file: File): TransferObserver {
        return transferManager.download(s3BucketName, serviceKey, file)
    }

    /**
     * Begin uploading a file.
     * @param serviceKey S3 service key
     * @param file Target file
     * @param metadata Object metadata to associate with upload
     * @return A transfer observer
     */
    override fun uploadFile(
        serviceKey: String,
        file: File,
        metadata: ObjectMetadata
    ): TransferObserver {
        return transferManager.upload(s3BucketName, serviceKey, file, metadata)
    }

    /**
     * Begin uploading an inputStream.
     * @param serviceKey S3 service key
     * @param inputStream Target InputStream
     * @param metadata Object metadata to associate with upload
     * @return A transfer observer
     * @throws IOException An IOException thrown during the process writing an InputStream into a file
     */
    override fun uploadInputStream(
        serviceKey: String,
        inputStream: InputStream,
        metadata: ObjectMetadata
    ): TransferObserver {
        val uploadOptions = UploadOptions(s3BucketName, metadata)
        return transferManager.upload(serviceKey, inputStream, uploadOptions)
    }

    /**
     * List items inside an S3 path.
     * @param path The path to list items from
     * @return A list of parsed items
     */
    override fun listFiles(path: String): MutableList<StorageItem> {
        val items = mutableListOf<StorageItem>()
        runBlocking {
            val result = s3Client.listObjectsV2Paginated {
                bucket = s3BucketName
                prefix = path
            }
            result.collect {
                it.contents?.forEach { value ->
                    val key = value.key
                    val lastModified = value.lastModified
                    val eTag = value.eTag
                    if (key != null && lastModified != null && eTag != null) items += StorageItem(
                        key,
                        value.size,
                        Date.from(Instant.ofEpochMilli(lastModified.epochSeconds)),
                        eTag,
                        null
                    )
                }
            }
        }
        return items
    }

    /**
     * Synchronous operation to delete a file in s3.
     * @param serviceKey Fully specified path to file to delete (including public/private/protected folder)
     */
    override fun deleteObject(serviceKey: String) {
        runBlocking {
            s3Client.deleteObject {
                bucket = s3BucketName
                key = serviceKey
            }
        }
    }

    /**
     * Pause a file transfer operation.
     * @param transferObserver an in-progress transfer
     */
    override fun pauseTransfer(transferObserver: TransferObserver) {
        transferManager.pause(transferObserver.id)
    }

    /**
     * Resume a file transfer.
     * @param transferObserver A transfer to be resumed
     */
    override fun resumeTransfer(transferObserver: TransferObserver) {
        transferManager.resume(transferObserver.id)
    }

    /**
     * Cancel a file transfer.
     * @param transferObserver A file transfer to cancel
     */
    override fun cancelTransfer(transferObserver: TransferObserver) {
        transferManager.cancel(transferObserver.id)
    }

    /**
     * Gets a handle of S3 client underlying this service.
     * @return S3 client instance
     */
    fun getClient(): S3Client {
        return s3Client
    }
}
