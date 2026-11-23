package dev.generalfullstack.filestorage.webflux.service

import dev.generalfullstack.filestorage.webflux.api.dto.CompleteImageUploadResponse
import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageDownloadResponse
import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageUploadRequest
import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageUploadResponse
import dev.generalfullstack.filestorage.webflux.api.dto.ImageStatus
import dev.generalfullstack.filestorage.webflux.api.dto.ImageVisibility
import dev.generalfullstack.filestorage.webflux.api.dto.MAX_IMAGE_SIZE_BYTES
import dev.generalfullstack.filestorage.webflux.api.error.ImageApiException
import dev.generalfullstack.filestorage.webflux.config.S3Properties
import dev.openfga.sdk.api.client.OpenFgaClient
import dev.openfga.sdk.api.client.model.ClientCheckRequest
import dev.openfga.sdk.api.client.model.ClientTupleKey
import dev.openfga.sdk.api.client.model.ClientTupleKeyWithoutCondition
import dev.openfga.sdk.api.client.model.ClientWriteRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3AsyncClient
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
import java.util.concurrent.RejectedExecutionException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

@Service
class ImageStorageService(
    private val s3AsyncClient: S3AsyncClient,
    private val s3Presigner: S3Presigner,
    private val openFgaClient: OpenFgaClient,
    private val s3Properties: S3Properties,
    @Qualifier("imageDecodeScheduler") private val imageDecodeScheduler: Scheduler,
) {
    fun createUpload(
        ownerId: String,
        request: CreateImageUploadRequest,
    ): Mono<CreateImageUploadResponse> =
        presign { prepareUpload(ownerId, request) }
            .flatMap { response -> writeOwner(response.imageId, ownerId).thenReturn(response) }

    fun completeUpload(
        ownerId: String,
        imageId: UUID,
    ): Mono<CompleteImageUploadResponse> {
        val key = stagingKey(imageId)
        return requireRelation(ownerId, imageId, "can_complete")
            .then(headObject(key))
            .flatMap { stagedHead ->
                val visibility = validateStagedUpload(stagedHead, ownerId)
                val size = stagedHead.contentLength()

                getStagedObject(key, size, stagedHead.eTag()).flatMap { stagedObject ->
                    val bytes = stagedObject.asByteArrayUnsafe()
                    validateImageAsync(bytes, size, stagedObject.response().contentType())
                        .flatMap { contentType -> putPublishedImage(imageId, ownerId, visibility, contentType, bytes) }
                }
            }
            .then(deleteObject(key))
            .thenReturn(CompleteImageUploadResponse(imageId, ImageStatus.AVAILABLE))
    }

    fun createDownload(
        imageId: UUID,
        requesterId: String?,
    ): Mono<CreateImageDownloadResponse> =
        headObject(imageKey(imageId)).flatMap { image ->
            val authorization = if (storedVisibility(image) == ImageVisibility.PRIVATE) {
                val userId = requesterId
                    ?: throw ImageApiException(HttpStatus.UNAUTHORIZED, "Sign-in is required to download this image.")
                requireRelation(userId, imageId, "can_download_private")
            } else {
                Mono.empty<Void>()
            }

            authorization.then(presign { signDownload(imageId) })
        }

    fun deleteImage(
        ownerId: String,
        imageId: UUID,
    ): Mono<Void> =
        headObject(imageKey(imageId))
            .flatMap { image ->
                requireRelation(ownerId, imageId, "can_delete")
                    .then(Mono.fromRunnable<Void> { validateDeletion(image, ownerId) })
            }
            .then(deleteObject(imageKey(imageId)))
            .then(deleteObject(stagingKey(imageId)))
            .then(deleteOwner(imageId, ownerId))

    fun grantViewer(
        ownerId: String,
        imageId: UUID,
        recipientUserId: UUID,
    ): Mono<Void> =
        headObject(imageKey(imageId))
            .then(requireRelation(ownerId, imageId, "can_delete"))
            .then(
                Mono.defer {
                    val recipientId = recipientUserId.toString()
                    if (recipientId == ownerId) {
                        throw ImageApiException(HttpStatus.BAD_REQUEST, "The owner already has access to this image.")
                    }
                    writeViewer(imageId, recipientId)
                },
            )

    fun revokeViewer(
        ownerId: String,
        imageId: UUID,
        recipientUserId: UUID,
    ): Mono<Void> =
        headObject(imageKey(imageId))
            .then(requireRelation(ownerId, imageId, "can_delete"))
            .then(deleteViewer(imageId, recipientUserId.toString()))

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
    ): Mono<Void> {
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

        return s3Call {
            s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(bytes))
        }.then()
    }

    private fun getStagedObject(key: String, size: Long, eTag: String?): Mono<ResponseBytes<GetObjectResponse>> =
        s3Call {
            s3AsyncClient.getObject(
                GetObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .ifMatch(eTag)
                    // S3 ranges are inclusive. Read one extra byte to detect an oversized object.
                    .range("bytes=0-$size")
                    .build(),
                AsyncResponseTransformer.toBytes(),
            )
        }

    private fun headObject(key: String): Mono<HeadObjectResponse> =
        s3Call {
            s3AsyncClient.headObject(
                HeadObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
            )
        }

    private fun deleteObject(key: String): Mono<Void> =
        s3Call {
            s3AsyncClient.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
            )
        }.then()

    private fun writeOwner(imageId: UUID, ownerId: String): Mono<Void> {
        val tuple = ClientTupleKey()
            .user(userObject(ownerId))
            .relation("owner")
            ._object(imageObject(imageId))

        return openFgaCall {
            openFgaClient.write(ClientWriteRequest().writes(listOf(tuple)))
        }.then()
    }

    private fun deleteOwner(imageId: UUID, ownerId: String): Mono<Void> {
        val tuple = ClientTupleKeyWithoutCondition()
            .user(userObject(ownerId))
            .relation("owner")
            ._object(imageObject(imageId))

        return openFgaCall {
            openFgaClient.write(ClientWriteRequest().deletes(listOf(tuple)))
        }.then()
    }

    private fun writeViewer(imageId: UUID, viewerId: String): Mono<Void> {
        val tuple = ClientTupleKey()
            .user(userObject(viewerId))
            .relation("viewer")
            ._object(imageObject(imageId))

        return openFgaCall {
            openFgaClient.write(ClientWriteRequest().writes(listOf(tuple)))
        }.then()
    }

    private fun deleteViewer(imageId: UUID, viewerId: String): Mono<Void> {
        val tuple = ClientTupleKeyWithoutCondition()
            .user(userObject(viewerId))
            .relation("viewer")
            ._object(imageObject(imageId))

        return openFgaCall {
            openFgaClient.write(ClientWriteRequest().deletes(listOf(tuple)))
        }.then()
    }

    private fun requireRelation(userId: String, imageId: UUID, relation: String): Mono<Void> {
        val request = ClientCheckRequest()
            .user(userObject(userId))
            .relation(relation)
            ._object(imageObject(imageId))

        return openFgaCall { openFgaClient.check(request) }
            .flatMap { response ->
                if (response.allowed == true) {
                    Mono.empty()
                } else {
                    Mono.error(ImageApiException(HttpStatus.FORBIDDEN, "You are not allowed to access this image."))
                }
            }
    }

    private fun <T : Any> openFgaCall(supplier: () -> CompletableFuture<T>): Mono<T> =
        Mono.fromFuture { supplier() }
            .onErrorMap { cause ->
                Exceptions.throwIfFatal(cause)
                ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "OpenFGA request failed.", cause)
            }

    private fun <T : Any> s3Call(supplier: () -> CompletableFuture<T>): Mono<T> =
        // The SDK can do synchronous work before it returns its future.
        Mono.fromFuture { supplier() }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorMap(SdkException::class.java) { cause ->
                when ((cause as? S3Exception)?.statusCode()) {
                    404 -> ImageApiException(HttpStatus.NOT_FOUND, "Image not found.", cause)
                    412 -> ImageApiException(HttpStatus.CONFLICT, "The staged upload changed during completion.", cause)
                    else -> ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "S3 request failed.", cause)
                }
            }

    private fun <T : Any> presign(supplier: () -> T): Mono<T> =
        Mono.fromCallable { supplier() }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorMap(SdkException::class.java) { cause ->
                ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "S3 URL signing failed.", cause)
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

    private fun validateImageAsync(bytes: ByteArray, size: Long, contentType: String?): Mono<String> =
        Mono.fromCallable { validateImage(bytes, size, contentType) }
            .subscribeOn(imageDecodeScheduler)
            .onErrorMap(RejectedExecutionException::class.java) { cause ->
                ImageApiException(HttpStatus.SERVICE_UNAVAILABLE, "Image validation is busy. Try again shortly.", cause)
            }

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
