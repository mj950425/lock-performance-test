package com.example.stockstudy.service.rdb

import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.entity.Stock
import com.example.stockstudy.enums.ServiceType
import com.example.stockstudy.service.StockDeductService
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/***
 * 남은 재고량을 RDBMS에서 관리하고, 비관적 잠금을 사용하여 재고량을 감소시키는 서비스
 */
@Service
class PessimisticDeductService(
    val entityManager: EntityManager
) : StockDeductService {

    @Transactional
    override fun deduct(deductCommandDto: DeductCommandDto) {
        val sortedIds = deductCommandDto.ids.sorted()

        val query = entityManager.createQuery(
            "SELECT s FROM Stock s WHERE s.id IN :ids",
            Stock::class.java
        )
        query.setParameter("ids", sortedIds)
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE)

        val lockedStocks = query.resultList

        lockedStocks.forEach { stock ->
            stock.deduct()
        }
    }

    override fun supports(serviceType: ServiceType): Boolean {
        return ServiceType.PESSIMISTIC == serviceType
    }
}
