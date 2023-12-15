package dev.crashteam.repricer.service

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemStrategyRepository
import dev.crashteam.repricer.repository.postgre.entity.strategy.KazanExpressAccountShopItemStrategyEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KeShopItemStrategyService(
    private val strategyRepository: KeAccountShopItemStrategyRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository
) {

    @Transactional
    fun saveStrategy(addStrategyRequest: AddStrategyRequest): Long {
        return keAccountShopItemRepository.saveStrategy(addStrategyRequest)
    }

    @Transactional
    fun findStrategy(shopItemId: UUID): KazanExpressAccountShopItemStrategyEntity? {
        return strategyRepository.findById(shopItemId)
    }

    @Transactional
    fun updateStrategy(shopItemId: UUID, patchStrategy: PatchStrategy): Int {
        return strategyRepository.update(shopItemId, patchStrategy)
    }

    @Transactional
    fun deleteStrategy(shopItemId: UUID): Int? {
        return strategyRepository.deleteById(shopItemId)
    }
}