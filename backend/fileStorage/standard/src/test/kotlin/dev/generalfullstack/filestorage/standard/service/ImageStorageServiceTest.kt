package dev.generalfullstack.filestorage.standard.service

import dev.generalfullstack.filestorage.standard.api.dto.CreateImageUploadRequest
import dev.generalfullstack.filestorage.standard.api.dto.ImageVisibility
import dev.generalfullstack.filestorage.standard.api.error.ImageApiException
import dev.generalfullstack.filestorage.standard.config.S3Properties
import dev.openfga.sdk.api.client.OpenFgaClient
import dev.openfga.sdk.api.client.model.ClientCheckRequest
import dev.openfga.sdk.api.client.model.ClientCheckResponse
import dev.openfga.sdk.api.client.model.ClientWriteRequest
import dev.openfga.sdk.api.client.model.ClientWriteResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.net.URL
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

class ImageStorageServiceTest {
    private val s3Client = Mockito.mock(S3Client::class.java)
    private val s3Presigner = Mockito.mock(S3Presigner::class.java)
    private val openFgaClient = Mockito.mock(OpenFgaClient::class.java)
    private val service = ImageStorageService(
        s3Client,
        s3Presigner,
        openFgaClient,
        S3Properties(
            endpoint = URI.create("http://localhost:8333"),
            presignedUrlEndpoint = URI.create("http://localhost:8333"),
            region = "us-east-1",
            bucket = "image-storage",
            accessKey = "test-access-key",
            secretKey = "test-secret-key",
            uploadUrlLifetime = Duration.ofMinutes(10),
            downloadUrlLifetime = Duration.ofMinutes(2),
        ),
    )

    @Test
    fun `anonymous caller can get a URL for a public image without OpenFGA check`() {
        val imageId = UUID.randomUUID()
        stubImageHead(ImageVisibility.PUBLIC)
        stubDownloadUrl("https://s3.example.test/images/$imageId")

        val response = service.createDownload(imageId, null)

        assertEquals("https://s3.example.test/images/$imageId", response.downloadUrl.toString())
        Mockito.verifyNoInteractions(openFgaClient)
    }

    @Test
    fun `owner can get a URL for a private image`() {
        val imageId = UUID.randomUUID()
        stubImageHead(ImageVisibility.PRIVATE)
        stubOpenFgaCheck(allowed = true)
        stubDownloadUrl("https://s3.example.test/images/$imageId")

        val response = service.createDownload(imageId, OWNER_ID)

        assertEquals("https://s3.example.test/images/$imageId", response.downloadUrl.toString())
        val checkRequest = ArgumentCaptor.forClass(ClientCheckRequest::class.java)
        Mockito.verify(openFgaClient).check(checkRequest.capture())
        assertEquals("user:$OWNER_ID", checkRequest.value.user)
        assertEquals("can_download_private", checkRequest.value.relation)
        assertEquals("image:$imageId", checkRequest.value.`object`)
    }

    @Test
    fun `anonymous caller cannot get a URL for a private image`() {
        val imageId = UUID.randomUUID()
        stubImageHead(ImageVisibility.PRIVATE)

        val exception = assertThrows(ImageApiException::class.java) {
            service.createDownload(imageId, null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        Mockito.verifyNoInteractions(openFgaClient, s3Presigner)
    }

    @Test
    fun `signed-in user without private image access is denied`() {
        val imageId = UUID.randomUUID()
        stubImageHead(ImageVisibility.PRIVATE)
        stubOpenFgaCheck(allowed = false)

        val exception = assertThrows(ImageApiException::class.java) {
            service.createDownload(imageId, OTHER_USER_ID)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.status)
        Mockito.verifyNoInteractions(s3Presigner)
    }

    @Test
    fun `owner can delete a public image`() {
        val imageId = UUID.randomUUID()
        stubImageHead(ImageVisibility.PUBLIC)
        stubOpenFgaCheck(allowed = true)
        stubOpenFgaWrite()
        Mockito.`when`(s3Client.deleteObject(any(DeleteObjectRequest::class.java)))
            .thenReturn(DeleteObjectResponse.builder().build())

        service.deleteImage(OWNER_ID, imageId)

        val checkRequest = ArgumentCaptor.forClass(ClientCheckRequest::class.java)
        Mockito.verify(openFgaClient).check(checkRequest.capture())
        assertEquals("can_delete", checkRequest.value.relation)
        val deleteRequests = ArgumentCaptor.forClass(DeleteObjectRequest::class.java)
        Mockito.verify(s3Client, Mockito.times(2)).deleteObject(deleteRequests.capture())
        assertEquals(
            setOf("images/$imageId", "staging/$imageId"),
            deleteRequests.allValues.map { it.key() }.toSet(),
        )

        val writeRequest = ArgumentCaptor.forClass(ClientWriteRequest::class.java)
        Mockito.verify(openFgaClient).write(writeRequest.capture())
        assertEquals("owner", writeRequest.value.deletes.first().relation)
    }

    @Test
    fun `user without ownership cannot delete an image`() {
        val imageId = UUID.randomUUID()
        stubImageHead(ImageVisibility.PRIVATE)
        stubOpenFgaCheck(allowed = false)

        val exception = assertThrows(ImageApiException::class.java) {
            service.deleteImage(OTHER_USER_ID, imageId)
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.status)
        Mockito.verify(s3Client, Mockito.never()).deleteObject(any(DeleteObjectRequest::class.java))
        Mockito.verify(openFgaClient, Mockito.never()).write(any(ClientWriteRequest::class.java))
    }

    @Test
    fun `upload request records the uploader and returns signed headers`() {
        val presignedRequest = Mockito.mock(PresignedPutObjectRequest::class.java)
        Mockito.`when`(presignedRequest.url()).thenReturn(URI.create("https://s3.example.test/staging").toURL())
        Mockito.`when`(presignedRequest.expiration()).thenReturn(Instant.parse("2030-01-01T00:10:00Z"))
        Mockito.`when`(presignedRequest.signedHeaders()).thenReturn(
            mapOf(
                "host" to listOf("s3.example.test"),
                "content-type" to listOf("image/png"),
                "x-amz-meta-owner-id" to listOf(OWNER_ID),
            ),
        )
        Mockito.`when`(s3Presigner.presignPutObject(any(PutObjectPresignRequest::class.java)))
            .thenReturn(presignedRequest)
        stubOpenFgaWrite()

        val response = service.createUpload(
            OWNER_ID,
            CreateImageUploadRequest(ImageVisibility.PRIVATE, "image/png", 100),
        )

        assertEquals("https://s3.example.test/staging", response.uploadUrl.toString())
        assertFalse(response.requiredHeaders.containsKey("host"))
        assertEquals("image/png", response.requiredHeaders["content-type"])
        val writeRequest = ArgumentCaptor.forClass(ClientWriteRequest::class.java)
        Mockito.verify(openFgaClient).write(writeRequest.capture())
        assertEquals("user:$OWNER_ID", writeRequest.value.writes.first().user)
        assertEquals("owner", writeRequest.value.writes.first().relation)
    }

    @Test
    fun `invalid uploaded bytes are not published`() {
        val imageId = UUID.randomUUID()
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val stagedObject = ResponseBytes.fromByteArray(
            GetObjectResponse.builder()
                .metadata(
                    mapOf(
                        "owner-id" to OWNER_ID,
                        "visibility" to "PRIVATE",
                        "declared-size" to bytes.size.toString(),
                    ),
                )
                .contentType("image/png")
                .contentLength(bytes.size.toLong())
                .build(),
            bytes,
        )
        Mockito.`when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(
            HeadObjectResponse.builder()
                .metadata(
                    mapOf(
                        "owner-id" to OWNER_ID,
                        "visibility" to "PRIVATE",
                        "declared-size" to bytes.size.toString(),
                    ),
                )
                .contentLength(bytes.size.toLong())
                .build(),
        )
        Mockito.`when`(s3Client.getObjectAsBytes(any(GetObjectRequest::class.java))).thenReturn(stagedObject)
        stubOpenFgaCheck(allowed = true)

        val exception = assertThrows(ImageApiException::class.java) {
            service.completeUpload(OWNER_ID, imageId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        Mockito.verify(s3Client, Mockito.never()).putObject(
            any(PutObjectRequest::class.java),
            any(software.amazon.awssdk.core.sync.RequestBody::class.java),
        )
    }

    @Test
    fun `valid image upload is copied to its published key and staging object is removed`() {
        val imageId = UUID.randomUUID()
        val bytes = createPng()
        val metadata = mapOf(
            "owner-id" to OWNER_ID,
            "visibility" to "PRIVATE",
            "declared-size" to bytes.size.toString(),
        )
        val stagedObject = ResponseBytes.fromByteArray(
            GetObjectResponse.builder()
                .metadata(metadata)
                .contentType("image/png")
                .contentLength(bytes.size.toLong())
                .build(),
            bytes,
        )
        Mockito.`when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(
            HeadObjectResponse.builder()
                .metadata(metadata)
                .contentLength(bytes.size.toLong())
                .build(),
        )
        Mockito.`when`(s3Client.getObjectAsBytes(any(GetObjectRequest::class.java))).thenReturn(stagedObject)
        Mockito.`when`(
            s3Client.putObject(
                any(PutObjectRequest::class.java),
                any(software.amazon.awssdk.core.sync.RequestBody::class.java),
            ),
        ).thenReturn(PutObjectResponse.builder().build())
        Mockito.`when`(s3Client.deleteObject(any(DeleteObjectRequest::class.java)))
            .thenReturn(DeleteObjectResponse.builder().build())
        stubOpenFgaCheck(allowed = true)

        val response = service.completeUpload(OWNER_ID, imageId)

        assertEquals(imageId, response.imageId)
        assertEquals("AVAILABLE", response.status.name)
        val publishedPut = ArgumentCaptor.forClass(PutObjectRequest::class.java)
        Mockito.verify(s3Client).putObject(
            publishedPut.capture(),
            any(software.amazon.awssdk.core.sync.RequestBody::class.java),
        )
        assertEquals("images/$imageId", publishedPut.value.key())
        assertEquals("PRIVATE", publishedPut.value.metadata()["visibility"])
        Mockito.verify(s3Client).deleteObject(any(DeleteObjectRequest::class.java))
    }

    private fun stubImageHead(visibility: ImageVisibility) {
        Mockito.`when`(s3Client.headObject(any(HeadObjectRequest::class.java))).thenReturn(
            HeadObjectResponse.builder()
                .metadata(mapOf("owner-id" to OWNER_ID, "visibility" to visibility.name))
                .contentType("image/png")
                .build(),
        )
    }

    private fun stubOpenFgaCheck(allowed: Boolean) {
        val response = Mockito.mock(ClientCheckResponse::class.java)
        Mockito.`when`(response.allowed).thenReturn(allowed)
        Mockito.`when`(openFgaClient.check(any(ClientCheckRequest::class.java)))
            .thenReturn(CompletableFuture.completedFuture(response))
    }

    private fun stubOpenFgaWrite() {
        val response = Mockito.mock(ClientWriteResponse::class.java)
        Mockito.`when`(openFgaClient.write(any(ClientWriteRequest::class.java)))
            .thenReturn(CompletableFuture.completedFuture(response))
    }

    private fun stubDownloadUrl(url: String) {
        val response = Mockito.mock(PresignedGetObjectRequest::class.java)
        Mockito.`when`(response.url()).thenReturn(URI.create(url).toURL())
        Mockito.`when`(response.expiration()).thenReturn(Instant.parse("2030-01-01T00:02:00Z"))
        Mockito.`when`(s3Presigner.presignGetObject(any(GetObjectPresignRequest::class.java)))
            .thenReturn(response)
    }

    private fun createPng(): ByteArray {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    private companion object {
        const val OWNER_ID = "11111111-1111-4111-8111-111111111111"
        const val OTHER_USER_ID = "22222222-2222-4222-8222-222222222222"
    }
}
