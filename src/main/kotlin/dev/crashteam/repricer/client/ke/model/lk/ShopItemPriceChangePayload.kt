package dev.crashteam.repricer.client.ke.model.lk

data class ShopItemPriceChangePayload(
    val productId: Long,
    val skuForProduct: String,
    val skuList: List<SkuPriceChangeSku>,
    val skuTitlesForCustomCharacteristics: List<Any> = emptyList()
)

data class SkuPriceChangeSku(
    val id: Long,
    val fullPrice: Long,
    val sellPrice: Long,
    val skuTitle: String,
    val barCode: String,
    val skuCharacteristicList: List<String> = emptyList(),
)
