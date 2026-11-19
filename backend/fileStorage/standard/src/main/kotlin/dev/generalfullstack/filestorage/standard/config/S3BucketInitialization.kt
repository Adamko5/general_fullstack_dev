package dev.generalfullstack.filestorage.standard.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CORSConfiguration
import software.amazon.awssdk.services.s3.model.CORSRule
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest
import software.amazon.awssdk.services.s3.model.S3Exception

@Configuration
class S3BucketInitialization {
    @Bean
    fun configureImageBucket(
        s3Client: S3Client,
        s3Properties: S3Properties,
        @Value("\${cors.allowed-origins}") allowedOrigins: List<String>,
    ): ApplicationRunner = ApplicationRunner {
        try {
            s3Client.headBucket(
                HeadBucketRequest.builder()
                    .bucket(s3Properties.bucket)
                    .build(),
            )
        } catch (exception: S3Exception) {
            if (exception.statusCode() != 404) {
                throw exception
            }

            s3Client.createBucket(
                CreateBucketRequest.builder()
                    .bucket(s3Properties.bucket)
                    .build(),
            )
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

        s3Client.putBucketCors(
            PutBucketCorsRequest.builder()
                .bucket(s3Properties.bucket)
                .corsConfiguration(corsConfiguration)
                .build(),
        )
    }
}
