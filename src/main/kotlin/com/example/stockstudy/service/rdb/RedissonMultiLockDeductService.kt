package com.example.stockstudy.service.rdb

import com.example.stockstudy.aop.annotation.DynamicDistributeLock
import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.enums.ServiceType
import com.example.stockstudy.repository.StockWithOptimisticRepository
import com.example.stockstudy.service.StockDeductService
import org.springframework.stereotype.Service

/***
 * 남은 재고량을 RDBMS에서 관리하고, redisson lock을 사용하여 재고량을 감소시키는 서비스
 */
@Service
class RedissonMultiLockDeductService(
    val stockWithOptimisticRepository: StockWithOptimisticRepository
) : StockDeductService {
    override fun supports(serviceType: ServiceType): Boolean {
        return ServiceType.DISTRIBUTION_MULTI_LOCK == serviceType
    }

    @DynamicDistributeLock("#deductCommandDto.ids")
    override fun deduct(deductCommandDto: DeductCommandDto) {
        try {
            val stocks = stockWithOptimisticRepository.findAllByIdIn(deductCommandDto.ids)

            require(stocks.size == deductCommandDto.ids.size)

            stocks.forEach { it.deduct() }

            stockWithOptimisticRepository.saveAll(stocks)
        } catch (e: Exception) {
            println("예외 발생: ${e.message}")
        }
    }
}
