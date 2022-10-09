package dev.crashteam.repricer.service.resolver

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import org.springframework.stereotype.Component

@Component
class RegexUrlToProductResolver(
    private val keShopItemRepository: KeShopItemRepository,
    private val kazanExpressWebClient: KazanExpressWebClient
) : UrlToProductResolver {

    override fun resolve(url: String): ResolvedKeProduct? {
        val lastSplitOfUrl = url.split("-").last()
        if (lastSplitOfUrl.contains("skuid")) {
            val findAll = "[0-9]+".toRegex().findAll(lastSplitOfUrl)
            val productId = findAll.first().value
            val skuId = findAll.last().value
            return ResolvedKeProduct(productId, skuId)
        } else {
            val productId = "[0-9]+".toRegex().find(lastSplitOfUrl)?.value
            if (productId != null) {
                val kazanExpressShopItemEntities = keShopItemRepository.findByProductId(productId.toLong())
                return if (kazanExpressShopItemEntities.isNotEmpty()) {
                    ResolvedKeProduct(productId, kazanExpressShopItemEntities.first().skuId.toString())
                } else {
                    val productResponse = kazanExpressWebClient.getProductInfo(productId)
                    productResponse?.payload?.data?.skuList?.first()?.let {
                        ResolvedKeProduct(productId, it.id.toString())
                    }
                }
            }
        }
        return null
    }
}
