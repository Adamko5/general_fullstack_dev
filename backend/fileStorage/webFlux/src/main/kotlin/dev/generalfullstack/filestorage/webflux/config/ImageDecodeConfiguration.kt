package dev.generalfullstack.filestorage.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@Configuration(proxyBeanMethods = false)
class ImageDecodeConfiguration {
    @Bean(destroyMethod = "dispose")
    fun imageDecodeScheduler(): Scheduler = Schedulers.newBoundedElastic(2, 8, "image-decode")
}
