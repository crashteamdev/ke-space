package dev.crashteam.repricer.client.ke.model.lk

data class AccountProductInfo(
    val category: AccountProductCategory,
    val title: String,
    val skuTitle: String
)

data class AccountProductCategory(
    val id: Long,
    val title: String
)
