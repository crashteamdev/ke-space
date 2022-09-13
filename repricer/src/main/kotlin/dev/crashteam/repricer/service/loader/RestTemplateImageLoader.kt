package dev.crashteam.repricer.service.loader

import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class RestTemplateImageLoader(
    private val restTemplate: RestTemplate
) : RemoteImageLoader {

    override fun loadResource(imageUrl: String): ByteArray {
        return restTemplate.getForObject(imageUrl, ByteArray::class.java)!!
    }
}
