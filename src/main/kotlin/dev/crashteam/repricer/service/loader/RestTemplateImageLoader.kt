package dev.crashteam.repricer.service.loader

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

private val log = KotlinLogging.logger {}

@Component
class RestTemplateImageLoader(
    private val restTemplate: RestTemplate
) : RemoteImageLoader {

    override fun loadResource(imageUrl: String): ByteArray {
        log.info { "Load shop item image from url: $imageUrl" }
        return restTemplate.getForObject(imageUrl, ByteArray::class.java)!!
    }
}
