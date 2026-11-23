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
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

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
        val response = presign { prepareUpload(ownerId, request) }
        writeOwner(response.imageId, ownerId)
        return response
    }

    fun completeUpload(
        ownerId: String,
        imageId: UUID,
    ): CompleteImageUploadResponse {
        val key = stagingKey(imageId)
        requireRelation(ownerId, imageId, "can_complete")
        val stagedHead = headObject(key)
        val visibility = validateStagedUpload(stagedHead, ownerId)
        val size = stagedHead.contentLength()

        val stagedObject = getStagedObject(key, size, stagedHead.eTag())
        val bytes = stagedObject.asByteArrayUnsafe()
        val contentType = validateImage(bytes, size, stagedObject.response().contentType())
        putPublishedImage(imageId, ownerId, visibility, contentType, bytes)

        deleteObject(key)
        return CompleteImageUploadResponse(imageId, ImageStatus.AVAILABLE)
    }

    fun createDownload(
        imageId: UUID,
        requesterId: String?,
    ): CreateImageDownloadResponse {
        val image = headObject(imageKey(imageId))
        if (storedVisibility(image) == ImageVisibility.PRIVATE) {
            val userId = requesterId
                ?: throw ImageApiException(HttpStatus.UNAUTHORIZED, "Sign-in is required to download this image.")
            requireRelation(userId, imageId, "can_download_private")
        }

        return presign { signDownload(imageId) }
    }

    fun deleteImage(
        ownerId: String,
        imageId: UUID,
    ) {
        val image = headObject(imageKey(imageId))
        requireRelation(ownerId, imageId, "can_delete")
        validateDeletion(image, ownerId)

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

    private fun prepareUpload(ownerId: String, request: CreateImageUploadRequest): CreateImageUploadResponse {
        validateUploadRequest(request)

        val imageId = UUID.randomUUID()
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(stagingKey(imageId))
            .contentType(request.contentType)
            .metadata(
                mapOf(
                    OWNER_ID_METADATA to ownerId,
                    VISIBILITY_METADATA to request.visibility.name,
                    DECLARED_SIZE_METADATA to request.sizeBytes.toString(),
                ),
            )
            .build()
        val presignedRequest = s3Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(s3Properties.uploadUrlLifetime)
                .putObjectRequest(putObjectRequest)
                .build(),
        )

        return CreateImageUploadResponse(
            imageId = imageId,
            uploadUrl = presignedRequest.url().toURI(),
            requiredHeaders = presignedRequest.signedHeaders()
                .filterKeys { !it.equals("host", ignoreCase = true) }
                .mapValues { (_, values) -> values.joinToString(",") },
            expiresAt = presignedRequest.expiration(),
        )
    }

    private fun signDownload(imageId: UUID): CreateImageDownloadResponse {
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

    private fun getStagedObject(key: String, size: Long, eTag: String?): ResponseBytes<GetObjectResponse> =
        s3Call {
            s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .ifMatch(eTag)
                    // S3 ranges are inclusive. Read one extra byte to detect an oversized object.
                    .range("bytes=0-$size")
                    .build(),
            )
        }

    private fun headObject(key: String): HeadObjectResponse =
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
            openFgaClient.write(ClientWriteRequest().writes(listOf(tuple)))
        }
    }

    private fun deleteOwner(imageId: UUID, ownerId: String) {
        val tuple = ClientTupleKeyWithoutCondition()
            .user(userObject(ownerId))
            .relation("owner")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().deletes(listOf(tuple)))
        }
    }

    private fun writeViewer(imageId: UUID, viewerId: String) {
        val tuple = ClientTupleKey()
            .user(userObject(viewerId))
            .relation("viewer")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().writes(listOf(tuple)))
        }
    }

    private fun deleteViewer(imageId: UUID, viewerId: String) {
        val tuple = ClientTupleKeyWithoutCondition()
            .user(userObject(viewerId))
            .relation("viewer")
            ._object(imageObject(imageId))

        openFgaCall {
            openFgaClient.write(ClientWriteRequest().deletes(listOf(tuple)))
        }
    }

    private fun requireRelation(userId: String, imageId: UUID, relation: String) {
        val request = ClientCheckRequest()
            .user(userObject(userId))
            .relation(relation)
            ._object(imageObject(imageId))

        val response = openFgaCall { openFgaClient.check(request) }
        if (response.allowed != true) {
            throw ImageApiException(HttpStatus.FORBIDDEN, "You are not allowed to access this image.")
        }
    }

    private fun <T : Any> openFgaCall(supplier: () -> CompletableFuture<T>): T =
        try {
            supplier().get()
        } catch (cause: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request was interrupted.", cause)
        } catch (exception: ExecutionException) {
            val cause = exception.cause ?: exception
            if (cause is Error) throw cause
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request failed.", cause)
        } catch (cause: Exception) {
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request failed.", cause)
        }

    private fun <T : Any> s3Call(supplier: () -> T): T =
        try {
            supplier()
        } catch (cause: SdkException) {
            throw when ((cause as? S3Exception)?.statusCode()) {
                404 -> ImageApiException(HttpStatus.NOT_FOUND, "Image not found.", cause)
                412 -> ImageApiException(HttpStatus.CONFLICT, "The staged upload changed during completion.", cause)
                else -> ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "S3 request failed.", cause)
            }
        }

    private fun <T : Any> presign(supplier: () -> T): T =
        try {
            supplier()
        } catch (cause: SdkException) {
            throw ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "S3 URL signing failed.", cause)
        }

    private fun validateStagedUpload(image: HeadObjectResponse, ownerId: String): ImageVisibility {
        val metadata = image.metadata()
        if (metadata[OWNER_ID_METADATA] != ownerId) {
            throw ImageApiException(HttpStatus.FORBIDDEN, "This upload belongs to another user.")
        }

        val declaredSize = metadata[DECLARED_SIZE_METADATA]?.toLongOrNull()
            ?: throw ImageApiException(HttpStatus.BAD_REQUEST, "The upload metadata is invalid.")
        val visibility = imageVisibility(metadata[VISIBILITY_METADATA])
            ?: throw ImageApiException(HttpStatus.BAD_REQUEST, "The upload metadata is invalid.")

        if (declaredSize !in 1..MAX_IMAGE_SIZE_BYTES || image.contentLength() != declaredSize) {
            throw sizeMismatch()
        }
        return visibility
    }

    private fun validateDeletion(image: HeadObjectResponse, ownerId: String) {
        if (image.metadata()[OWNER_ID_METADATA] != ownerId) {
            throw ImageApiException(HttpStatus.FORBIDDEN, "Only the uploader can delete this image.")
        }
        storedVisibility(image)
    }

    private fun storedVisibility(image: HeadObjectResponse): ImageVisibility =
        imageVisibility(image.metadata()[VISIBILITY_METADATA])
            ?: throw ImageApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The stored image metadata is invalid.")

    private fun imageVisibility(value: String?): ImageVisibility? =
        ImageVisibility.entries.firstOrNull { it.name == value }

    private fun sizeMismatch(): ImageApiException =
        ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded image size does not match its declaration.")

    private fun validateImage(bytes: ByteArray, size: Long, contentType: String?): String {
        if (bytes.size.toLong() != size) throw sizeMismatch()

        val detectedContentType = detectImageContentType(bytes)
            ?: throw ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded bytes are not a supported image.")
        if (contentType != detectedContentType) {
            throw ImageApiException(HttpStatus.BAD_REQUEST, "The uploaded image content type does not match its bytes.")
        }
        return detectedContentType
    }

    private fun detectImageContentType(bytes: ByteArray): String? {
        MemoryCacheImageInputStream(ByteArrayInputStream(bytes)).use { input ->
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
