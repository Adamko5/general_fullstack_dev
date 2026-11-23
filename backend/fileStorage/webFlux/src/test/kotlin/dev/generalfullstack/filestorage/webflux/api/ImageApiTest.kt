package dev.generalfullstack.filestorage.webflux.api

import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageDownloadResponse
import dev.generalfullstack.filestorage.webflux.service.ImageStorageService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "openfga.store-id=01ARZ3NDEKTSV4RRFFQ69G5FAV",
        "openfga.authorization-model-id=01ARZ3NDEKTSV4RRFFQ69G5FAW",
    ],
)
class ImageApiTest {
    @LocalServerPort
    private var port: Int = 0

    @MockitoBean(name = "configureImageBucket", enforceOverride = true)
    private lateinit var bucketInitializer: ApplicationRunner

    @MockitoBean(enforceOverride = true)
    private lateinit var imageStorageService: ImageStorageService

    @Test
    fun `security allows anonymous downloads and requires authentication for deletion`() {
        val imageId = UUID.randomUUID()
        val downloadUrl = "https://s3.example.test/images/$imageId"
        val expiresAt = Instant.parse("2030-01-01T00:02:00Z")
        Mockito.`when`(imageStorageService.createDownload(imageId, null))
            .thenReturn(Mono.just(CreateImageDownloadResponse(URI.create(downloadUrl), expiresAt)))
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

        client.get().uri("/images/{id}/download", imageId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.downloadUrl").isEqualTo(downloadUrl)
            .jsonPath("$.expiresAt").isEqualTo(expiresAt.toString())

        client.delete().uri("/images/{id}", imageId)
            .exchange()
            .expectStatus().isUnauthorized
        Mockito.verify(imageStorageService).createDownload(imageId, null)
        Mockito.verifyNoMoreInteractions(imageStorageService)
    }
}
