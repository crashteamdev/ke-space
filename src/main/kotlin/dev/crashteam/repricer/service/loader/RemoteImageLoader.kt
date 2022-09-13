package dev.crashteam.repricer.service.loader

interface RemoteImageLoader {
    fun loadResource(imageUrl: String): ByteArray
}
