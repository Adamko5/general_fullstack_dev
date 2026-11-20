package dev.generalfullstack.filestorage.standard.service

import dev.generalfullstack.filestorage.standard.api.dto.CompleteImageUploadResponse
import dev.generalfullstack.filestorage.standard.api.dto.CreateImageDownloadResponse
import dev.generalfullstack.filestorage.standard.api.dto.CreateImageUploadRequest
import dev.generalfullstack.filestorage.standard.api.dto.CreateImageUploadResponse
import dev.generalfullstack.filestorage.standard.api.dto.ImageStatus
import dev.generalfullstack.filestorage.standard.api.dto.ImageVisibility
import dev.generalfullstack.filestorage.standard.api.dto.MAX_IMAGE_SIZE_BYTES
import dev.generalfullstack.filestorage.standard.api.error.ImageApiException
import dev.generalfullstack.filestorage.standard.config.S3Properties
import dev.openfga.sdk.api.client.OpenFgaClient
import dev.openfga.sdk.api.client.model.ClientCheckRequest
import dev.openfga.sdk.api.client.model.ClientTupleKey
import dev.openfga.sdk.api.client.model.ClientTupleKeyWithoutCondition
import dev.openfga.sdk.api.client.model.ClientWriteRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.UUID
import javax.imageio.ImageIO

@Service
class ImageStorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val openFgaClient: OpenFgaClient,
    private val s3Properties: S3Properties,
) {
    fun createUpload(
        ownerId: String,
        request: CreateImageUploadRequest,
    ): CreateImageUploadResponse {
        validateUploadRequest(request)

        val imageId = UUID.randomUUID()
        val key = stagingKey(imageId)
        val metadata = mapOf(
            OWNER_ID_METADATA to ownerId,
            VISIBILITY_METADATA to request.visibility.name,
            DECLARED_SIZE_METADATA to request.sizeBytes.toString(),
        )
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentType(request.contentType)
            .metadata(metadata)
            .build()
        val presignedRequest = s3Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(s3Properties.uploadUrlLifetime)
                .putObjectRequest(putObjectRequest)
                .build(),
        )

        writeOwner(imageId, ownerId)

        return CreateImageUploadResponse(
            imageId = imageId,
            uploadUrl = presignedRequest.url().toURI(),
            requiredHeaders = presignedRequest.signedHeaders()
                .filterKeys { it.lowercase() != "host" }
                .mapValues { (_, values) -> values.joinToString(",") },
            expiresAt = presignedRequest.expiration(),
        )
    }

    fun completeUpload(
        ownerId: String,
        imageId: UUID,
    ): CompleteImageUploadResponse {
        requireRelation(ownerId, imageId, "can_complete")

        val key = stagingKey(imageId)
        val stagedHead = headObject(key)
        val metadata = stagedHead.metadata()
        if (metadata[OWNER_ID_METADATA] != ownerId) {
            throw ImageApiException(HttpStatus.FORBIDDEN, "This upload belongs to another user.")
        }

        val declaredSize = metadata[DECLARED_SIZE_METADATA]?.toLongOrNull()
            ?: throw ImageApiException(HttpStatus.BAD_REQUEST, "The upload metadata is invalid.")
        val visibility = metadata[VISIBILITY_METADATA]?.let(::parseVisibility)
            ?: throw ImageApiException(HttpStatus.BAD_REQUEST, "The upload metadata is invalid.")

        if (stagedHead.contentLength() != declaredSize || declaredSize !in 1..MAX_IMAGE_SIZE_BYTES) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded image size does not match its declaration.")
        }

        val stagedObject = getObject(key)
        val bytes = stagedObject.asByteArray()
        if (bytes.isEmpty() || bytes.size.toLong() != declaredSize) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded image size does not match its declaration.")
        }

        val actualContentType = detectImageContentType(bytes)
            ?: throw ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded bytes are not a supported image.")
        if (stagedObject.response().contentType() != actualContentType) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded image content type does not match its bytes.")
        }

        putPublishedImage(imageId, ownerId, visibility, actualContentType, bytes)
        deleteObject(stagingKey(imageId))

        return CompleteImageUploadResponse(imageId, ImageStatus.AVAILABLE)
    }

    fun createDownload(
        imageId: UUID,
        requesterId: String?,
    ): CreateImageDownloadResponse {
        val image = headObject(imageKey(imageId))
        val visibility = image.metadata()[VISIBILITY_METADATA]?.let(::parseVisibility)
            ?: throw ImageApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The stored image metadata is invalid.")

        if (visibility == ImageVisibility.PRIVATE) {
            val userId = requesterId
                ?: throw ImageApiException(HttpStatus.UNAUTHORIZED, "Sign-in is required to download this image.")
            requireRelation(userId, imageId, "can_download_private")
        }

        val request = GetObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(imageKey(imageId))
            .build()
        val presignedRequest = s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(s3Properties.downloadUrlLifetime)
                .getObjectRequest(request)
                .build(),
        )

        return CreateImageDownloadResponse(
            downloadUrl = presignedRequest.url().toURI(),
            expiresAt = presignedRequest.expiration(),
        )
    }

    fun deleteImage(
        ownerId: String,
        imageId: UUID,
    ) {
        val image = headObject(imageKey(imageId))
        requireRelation(ownerId, imageId, "can_delete")
        if (image.metadata()[OWNER_ID_METADATA] != ownerId) {
            throw ImageApiException(HttpStatus.FORBIDDEN, "Only the uploader can delete this image.")
        }
        parseVisibility(image.metadata()[VISIBILITY_METADATA]
            ?: throw ImageApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The stored image metadata is invalid."))

        deleteObject(imageKey(imageId))
        deleteObject(stagingKey(imageId))
        deleteOwner(imageId, ownerId)
    }

    fun grantViewer(
        ownerId: String,
        imageId: UUID,
        recipientUserId: UUID,
    ) {
        headObject(imageKey(imageId))
        requireRelation(ownerId, imageId, "can_delete")

        val recipientId = recipientUserId.toString()
        if (recipientId == ownerId) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "The owner already has access to this image.")
        }

        writeViewer(imageId, recipientId)
    }

    fun revokeViewer(
        ownerId: String,
        imageId: UUID,
        recipientUserId: UUID,
    ) {
        headObject(imageKey(imageId))
        requireRelation(ownerId, imageId, "can_delete")

        deleteViewer(imageId, recipientUserId.toString())
    }

    private fun validateUploadRequest(request: CreateImageUploadRequest) {
        if (request.sizeBytes !in 1..MAX_IMAGE_SIZE_BYTES) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "Image size must be between 1 byte and 5 MiB.")
        }
        if (request.contentType != JPEG_CONTENT_TYPE && request.contentType != PNG_CONTENT_TYPE) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are supported.")
        }
    }

    private fun putPublishedImage(
        imageId: UUID,
        ownerId: String,
        visibility: ImageVisibility,
        contentType: String,
        bytes: ByteArray,
    ) {
        val request = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(imageKey(imageId))
            .contentType(contentType)
            .metadata(
                mapOf(
                    OWNER_ID_METADATA to ownerId,
                    VISIBILITY_METADATA to visibility.name,
                ),
            )
            .build()

        s3Call {
            s3Client.putObject(request, RequestBody.fromBytes(bytes))
        }
    }

    private fun getObject(key: String) =
        s3Call {
            s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
            )
        }

    private fun headObject(key: String) =
        s3Call {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
            )
        }

    private fun deleteObject(key: String) {
        s3Call {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
            )
        }
    }

    private fun writeOwner(imageId: UUID, ownerId: String) {
        val tuple = ClientTupleKey()
            .user(userObject(ownerId))
            .relation("owner")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().writes(listOf(tuple))).get()
        }
    }

    private fun deleteOwner(imageId: UUID, ownerId: String) {
        val tuple = ClientTupleKeyWithoutCondition()
            .user(userObject(ownerId))
            .relation("owner")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().deletes(listOf(tuple))).get()
        }
    }

    private fun writeViewer(imageId: UUID, viewerId: String) {
        val tuple = ClientTupleKey()
            .user(userObject(viewerId))
            .relation("viewer")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().writes(listOf(tuple))).get()
        }
    }

    private fun deleteViewer(imageId: UUID, viewerId: String) {
        val tuple = ClientTupleKeyWithoutCondition()
            .user(userObject(viewerId))
            .relation("viewer")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().deletes(listOf(tuple))).get()
        }
    }

    private fun requireRelation(userId: String, imageId: UUID, relation: String) {
        val request = ClientCheckRequest()
            .user(userObject(userId))
            .relation(relation)
            ._object(imageObject(imageId))

        val allowed = openFgaCall { openFgaClient.check(request).get().allowed == true }
        if (!allowed) {
            throw ImageApiException(HttpStatus.FORBIDDEN, "You are not allowed to access this image.")
        }
    }

    private fun <T> openFgaCall(operation: () -> T): T =
        try {
            operation()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request was interrupted.")
        } catch (exception: ExecutionException) {
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request failed.")
        } catch (exception: Exception) {
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request failed.")
        }

    private fun <T> s3Call(operation: () -> T): T =
        try {
            operation()
        } catch (exception: S3Exception) {
            if (exception.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw ImageApiException(HttpStatus.NOT_FOUND, "Image not found.")
            }
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "S3 request failed.")
        } catch (exception: SdkException) {
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "S3 request failed.")
        }

    private fun parseVisibility(value: String): ImageVisibility =
        try {
            ImageVisibility.valueOf(value)
        } catch (exception: IllegalArgumentException) {
            throw ImageApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The stored image metadata is invalid.")
        }

    private fun detectImageContentType(bytes: ByteArray): String? {
        val imageInput = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return null
        imageInput.use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return null

            val reader = readers.next()
            try {
                reader.input = input
                val contentType = when (reader.formatName.lowercase()) {
                    "jpeg", "jpg" -> JPEG_CONTENT_TYPE
                    "png" -> PNG_CONTENT_TYPE
                    else -> return null
                }

                val pixelCount = reader.getWidth(0).toLong() * reader.getHeight(0).toLong()
                if (pixelCount > MAX_IMAGE_PIXEL_COUNT) {
                    throw ImageApiException(HttpStatus.CONTENT_TOO_LARGE, "The image dimensions are too large.")
                }

                reader.read(0) ?: return null

                return contentType
            } catch (exception: IOException) {
                return null
            } finally {
                reader.dispose()
            }
        }
    }

    private fun stagingKey(imageId: UUID): String = "staging/$imageId"

    private fun imageKey(imageId: UUID): String = "images/$imageId"

    private fun userObject(userId: String): String = "user:$userId"

    private fun imageObject(imageId: UUID): String = "image:$imageId"

    private companion object {
        const val OWNER_ID_METADATA = "owner-id"
        const val VISIBILITY_METADATA = "visibility"
        const val DECLARED_SIZE_METADATA = "declared-size"
        const val JPEG_CONTENT_TYPE = "image/jpeg"
        const val PNG_CONTENT_TYPE = "image/png"
        const val MAX_IMAGE_PIXEL_COUNT = 16_000_000L
    }
}
