package com.example.stockstudy.service.rdb

import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.entity.StockWithOptimisticLock
import com.example.stockstudy.enums.ServiceType
import com.example.stockstudy.service.StockDeductService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/***
 * 남은 재고량을 RDBMS에서 관리하고, 낙관적 잠금을 사용하여 재고량을 감소시키는 서비스
 */
@Service
class OptimisticDeductService(
    val entityManager: EntityManager
) : StockDeductService {
    @Transactional
    override fun deduct(deductCommandDto: DeductCommandDto) {
        val maxRetries = 3
        var currentTry = 0
        var waitTime = 50L

        val sortedIds = deductCommandDto.ids.sorted()

        for (id in sortedIds) {
            while (true) {
                try {
                    val stock = entityManager.find(StockWithOptimisticLock::class.java, id)
                    stock.deduct()
                    entityManager.flush()
                    break
                } catch (e: Exception) {
                    currentTry++

                    if (currentTry >= maxRetries) {
                        println("재고 감소 작업에 실패했습니다. 최대 재시도 횟수를 초과했습니다.")
                        throw RuntimeException("재고 감소 작업에 실패했습니다. 최대 재시도 횟수를 초과했습니다.")
                    }

                    Thread.sleep(waitTime)
                    waitTime *= 10

                    entityManager.refresh(entityManager.find(StockWithOptimisticLock::class.java, id))
                }
            }
        }
    }

    override fun supports(serviceType: ServiceType): Boolean {
        return ServiceType.OPTIMISTIC == serviceType
    }
}
