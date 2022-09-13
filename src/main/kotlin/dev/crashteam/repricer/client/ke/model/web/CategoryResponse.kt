package dev.crashteam.repricerjober.client.model

import java.math.BigDecimal

data class CategoryResponse(
    val payload: CategoryPayload?,
    val error: String?
)

data class CategoryPayload(
    val products: List<CategoryProduct>,
    val adultContent: Boolean,
    val totalProducts: Int
)

data class CategoryProduct(
    val productId: Long,
    val title: String,
    val sellPrice: BigDecimal,
    val fullPrice: BigDecimal?,
    val compressedImage: String,
    val image: String,
    val rating: BigDecimal,
    val ordersQuantity: Long,
    val rOrdersQuantity: Long,
)
