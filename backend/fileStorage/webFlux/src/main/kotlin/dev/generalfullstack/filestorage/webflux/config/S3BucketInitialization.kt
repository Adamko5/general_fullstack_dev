package dev.generalfullstack.filestorage.webflux.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CORSConfiguration
import software.amazon.awssdk.services.s3.model.CORSRule
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.util.concurrent.CompletionException

@Configuration
class S3BucketInitialization {
    @Bean
    fun configureImageBucket(
        s3AsyncClient: S3AsyncClient,
        s3Properties: S3Properties,
        @Value("\${cors.allowed-origins}") allowedOrigins: List<String>,
    ): ApplicationRunner = ApplicationRunner {
        try {
            s3AsyncClient.headBucket(
                HeadBucketRequest.builder()
                    .bucket(s3Properties.bucket)
                    .build(),
            ).join()
        } catch (exception: CompletionException) {
            val s3Exception = exception.cause as? S3Exception
            if (s3Exception?.statusCode() != 404) throw exception

            s3AsyncClient.createBucket(
                CreateBucketRequest.builder()
                    .bucket(s3Properties.bucket)
                    .build(),
            ).join()
        }

        val corsRule = CORSRule.builder()
            .allowedOrigins(allowedOrigins)
            .allowedMethods(listOf("GET", "PUT", "HEAD"))
            .allowedHeaders(listOf("*"))
            .exposeHeaders(listOf("ETag"))
            .maxAgeSeconds(3600)
            .build()
        val corsConfiguration = CORSConfiguration.builder()
            .corsRules(corsRule)
            .build()

        s3AsyncClient.putBucketCors(
            PutBucketCorsRequest.builder()
                .bucket(s3Properties.bucket)
                .corsConfiguration(corsConfiguration)
                .build(),
        ).join()
    }
}
