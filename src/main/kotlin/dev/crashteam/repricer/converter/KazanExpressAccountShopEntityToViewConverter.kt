package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountShop
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import org.springframework.stereotype.Component

@Component
class KazanExpressAccountShopEntityToViewConverter : DataConverter<KazanExpressAccountShopEntity, KeAccountShop> {

    override fun convert(source: KazanExpressAccountShopEntity): KeAccountShop {
        return KeAccountShop().apply {
            id = source.id
            name = source.name
        }
    }
}
