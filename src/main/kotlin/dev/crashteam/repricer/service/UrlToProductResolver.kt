package dev.crashteam.repricer.service

interface UrlToProductResolver {
    fun resolve(url: String): ResolvedKeProduct?
}

data class ResolvedKeProduct(
    val productId: String,
    val skuId: String
)
