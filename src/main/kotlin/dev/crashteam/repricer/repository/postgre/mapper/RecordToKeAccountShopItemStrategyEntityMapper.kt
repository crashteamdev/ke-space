package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShopItemStrategy.KE_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.repricer.db.model.tables.StrategyOption.STRATEGY_OPTION
import dev.crashteam.repricer.repository.postgre.entity.strategy.KazanExpressAccountShopItemStrategyEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKeAccountShopItemStrategyEntityMapper : RecordMapper<KazanExpressAccountShopItemStrategyEntity> {
    override fun convert(record: Record): KazanExpressAccountShopItemStrategyEntity {
        return KazanExpressAccountShopItemStrategyEntity(
            record.getValue(KE_ACCOUNT_SHOP_ITEM_STRATEGY.ID),
            record.getValue(KE_ACCOUNT_SHOP_ITEM_STRATEGY.STRATEGY_TYPE).literal,
            record.getValue(STRATEGY_OPTION.ID),
            record.getValue(STRATEGY_OPTION.MINIMUM_THRESHOLD),
            record.getValue(STRATEGY_OPTION.MAXIMUM_THRESHOLD),
            record.getValue(STRATEGY_OPTION.STEP),
            record.getValue(STRATEGY_OPTION.DISCOUNT),
            record.getValue(KE_ACCOUNT_SHOP_ITEM_STRATEGY.KE_ACCOUNT_SHOP_ITEM_ID),
            record.getValue(STRATEGY_OPTION.CHANGE_NOT_AVAILABLE_ITEM_PRICE),
            record.getValue(STRATEGY_OPTION.COMPETITOR_AVAILABLE_AMOUNT),
            record.getValue(STRATEGY_OPTION.COMPETITOR_SALES_AMOUNT)
        )
    }
}