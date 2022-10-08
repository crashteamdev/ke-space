package dev.crashteam.repricer.client.ke.model.lk

data class ShopItemPriceChangePayload(
    val productId: Long,
    val skuForProduct: String,
    val skuList: List<SkuPriceChangeSku>,
    val skuTitlesForCustomCharacteristics: List<SkuTitleCharacteristic> = emptyList()
)

data class SkuPriceChangeSku(
    val id: Long,
    val fullPrice: Long,
    val sellPrice: Long,
    val skuTitle: String,
    val barCode: String,
    val skuCharacteristicList: List<SkuCharacteristic> = emptyList(),
)

data class SkuTitleCharacteristic(
    val characteristicTitle: String,
    val customCharacteristicValuesSkus: List<CustomCharacteristicSkuValue>
)

data class CustomCharacteristicSkuValue(
    val customCharacteristicValueTitle: String,
    val skuValue: String
)

data class SkuCharacteristic(
    val characteristicTitle: String,
    val definedType: Boolean,
    val characteristicValue: String
)
