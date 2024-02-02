package dev.crashteam.repricer.client.ke.model.lk

data class AccountProductDescription(
    val id: Long,
    val shopSkuTitle: String,
    val title: String,
    val productSkuTitle: String,
    val commission: Short,
    val hasActiveCalendarEvents: Boolean,
    val hasCustomCharacteristics: Boolean,
    val definedCharacteristicList: List<DefinedCharacteristic>? = null,
    val customCharacteristicList: List<CustomCharacteristic>? = null,
    val skuList: List<AccountProductSku>
)

data class DefinedCharacteristic(
    val characteristicTitle: String,
    val orderingNumber: Int,
    val characteristicValues: List<DefinedCharacteristicValue>
)

data class DefinedCharacteristicValue(
    val title: String,
    val value: String,
    val skuValue: String
)

data class CustomCharacteristic(
    val characteristicTitle: String,
    val characteristicValues: List<CustomCharacteristicValue>
)

data class CustomCharacteristicValue(
    val title: String,
    val value: String,
    val skuValue: String
)

data class AccountProductSku(
    val id: Long,
    val skuTitle: String,
    val quantityCreated: Int,
    val fullPrice: Long,
    val sellPrice: Long,
    val barcode: String,
    val skuCharacteristicList: List<AccountProductCharacteristic>,
    val isActive: Boolean,
    val canEdit: Boolean,
    val blocked: Boolean,
    val status: AccountProductSkuStatus
)

data class AccountProductCharacteristic(
    val characteristicTitle: String,
    val definedType: Boolean,
    val characteristicValue: String,
)

data class AccountProductSkuStatus(
    val value: String,
    val title: String,
    val color: String
)
