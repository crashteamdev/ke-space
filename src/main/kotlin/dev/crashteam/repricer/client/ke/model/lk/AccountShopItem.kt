package dev.crashteam.repricer.client.ke.model.lk

import java.math.BigDecimal

data class AccountShopItemWrapper(
    val productList: List<AccountShopItem>
)

data class AccountShopItem(
    val productId: Long,
    val title: String,
    val skuTitle: String,
    val category: String,
    val status: ShopItemStatus,
    val moderationStatus: ShopItemModerationStatus,
    val commission: BigDecimal,
    val commissionDto: ShopItemCommission,
    val skuList: List<ShopItemSku>,
    val image: String,
)

data class ShopItemStatus(
    val title: String,
    val value: String,
)

data class ShopItemModerationStatus(
    val title: String,
    val value: String,
)

data class ShopItemCommission(
    val minCommission: Long,
    val maxCommission: Long
)

data class ShopItemSku(
    val skuTitle: String,
    val skuFullTitle: String,
    val productTitle: String,
    val skuId: Long,
    val barcode: Long,
    val purchasePrice: BigDecimal?,
    val price: BigDecimal,
    val quantityActive: Long,
    val quantityAdditional: Long,
)
