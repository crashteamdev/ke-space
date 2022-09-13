package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShopItemPriceHistory.KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopItemPriceHistoryEntityMapper :
    RecordMapper<KazanExpressShopItemPriceHistoryEntity> {

    override fun convert(record: Record): KazanExpressShopItemPriceHistoryEntity {
        return KazanExpressShopItemPriceHistoryEntity(
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_ID),
            keAccountShopItemCompetitorId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_COMPETITOR_ID),
            changeTime = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.CHANGE_TIME),
            oldPrice = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
            price = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE)
        )
    }
}
