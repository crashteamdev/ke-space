package dev.crashteam.repricer.service

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemStrategyRepository
import dev.crashteam.repricer.repository.postgre.entity.strategy.KazanExpressAccountShopItemStrategyEntity
import org.springframework.stereotype.Service

@Service
class KeShopItemStrategyService(
    private val strategyRepository: KeAccountShopItemStrategyRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository
) {

    fun saveStrategy(addStrategyRequest: AddStrategyRequest): Long {
        return keAccountShopItemRepository.saveStrategy(addStrategyRequest)
    }

    fun findStrategy(id: Long): KazanExpressAccountShopItemStrategyEntity? {
        return strategyRepository.findById(id)
    }

    fun updateStrategy(shopItemStrategyId: Long, patchStrategy: PatchStrategy): Int {
        return strategyRepository.update(shopItemStrategyId, patchStrategy)
    }

    fun deleteStrategy(id: Long): Int? {
        return strategyRepository.deleteById(id)
    }
}