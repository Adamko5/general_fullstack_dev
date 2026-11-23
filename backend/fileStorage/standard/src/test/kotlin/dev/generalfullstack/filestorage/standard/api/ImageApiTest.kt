package dev.generalfullstack.filestorage.standard.api

import com.jayway.jsonpath.JsonPath
import dev.generalfullstack.filestorage.standard.api.dto.CreateImageDownloadResponse
import dev.generalfullstack.filestorage.standard.service.ImageStorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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
        val imageUrl = "http://localhost:$port/images/$imageId"
        val downloadUrl = "https://s3.example.test/images/$imageId"
        val expiresAt = Instant.parse("2030-01-01T00:02:00Z")
        Mockito.`when`(imageStorageService.createDownload(imageId, null))
            .thenReturn(CreateImageDownloadResponse(URI.create(downloadUrl), expiresAt))

        HttpClient.newHttpClient().use { client ->
            val download = HttpRequest.newBuilder(URI.create("$imageUrl/download"))
                .timeout(Duration.ofSeconds(5))
                .build()
            val response = client.send(download, HttpResponse.BodyHandlers.ofString())
            assertEquals(200, response.statusCode())
            assertEquals(downloadUrl, JsonPath.read<String>(response.body(), "$.downloadUrl"))
            assertEquals(expiresAt.toString(), JsonPath.read<String>(response.body(), "$.expiresAt"))

            val delete = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(5))
                .DELETE()
                .build()
            assertEquals(401, client.send(delete, HttpResponse.BodyHandlers.discarding()).statusCode())
        }
        Mockito.verify(imageStorageService).createDownload(imageId, null)
        Mockito.verifyNoMoreInteractions(imageStorageService)
    }
}
