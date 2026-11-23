package dev.generalfullstack.filestorage.webflux.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration as AwsS3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration

@Configuration
@EnableConfigurationProperties(S3Properties::class)
class S3Configuration {
    @Bean(destroyMethod = "close")
    fun s3AsyncClient(properties: S3Properties): S3AsyncClient =
        S3AsyncClient.builder()
            .endpointOverride(properties.endpoint)
            .region(Region.of(properties.region))
            .credentialsProvider(properties.credentialsProvider())
            .serviceConfiguration(s3ServiceConfiguration())
            .overrideConfiguration { config ->
                config.apiCallAttemptTimeout(Duration.ofSeconds(10))
                    .apiCallTimeout(Duration.ofSeconds(30))
            }
            .build()

    @Bean(destroyMethod = "close")
    fun s3Presigner(properties: S3Properties): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(properties.presignedUrlEndpoint)
            .region(Region.of(properties.region))
            .credentialsProvider(properties.credentialsProvider())
            .serviceConfiguration(s3ServiceConfiguration())
            .build()

    private fun S3Properties.credentialsProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

    private fun s3ServiceConfiguration(): AwsS3Configuration =
        AwsS3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build()
}
