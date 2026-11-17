package dev.generalfullstack.auth.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.security.converter.RsaKeyConverters
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

@Configuration
class JwtKeyConfiguration {
    @Bean
    fun oidcRsaKey(properties: OidcProperties, resourceLoader: ResourceLoader): RSAKey {
        val publicKey = resourceLoader.getResource(properties.publicKeyLocation)
            .inputStream
            .use { requireNotNull(RsaKeyConverters.x509().convert(it)) as RSAPublicKey }

        val privateKey = resourceLoader.getResource(properties.privateKeyLocation)
            .inputStream
            .use { requireNotNull(RsaKeyConverters.pkcs8().convert(it)) as RSAPrivateKey }

        return RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(properties.signingKeyId)
            .build()
    }

    @Bean
    fun jwkSource(oidcRsaKey: RSAKey): JWKSource<SecurityContext> =
        ImmutableJWKSet(JWKSet(oidcRsaKey))
}
