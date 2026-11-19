package dev.generalfullstack.filestorage.standard.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("storage.s3")
data class S3Properties(
    val endpoint: URI,
    val presignedUrlEndpoint: URI,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    val uploadUrlLifetime: Duration,
    val downloadUrlLifetime: Duration,
)
