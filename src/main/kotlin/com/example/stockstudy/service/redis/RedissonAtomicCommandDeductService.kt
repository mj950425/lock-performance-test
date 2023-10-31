package com.example.stockstudy.service.redis

import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.enums.ServiceType
import com.example.stockstudy.service.StockDeductService
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

/***
 * 남은 재고량을 Redis에서 관리하고 Redisson AtomicLong을 사용하여 재고량을 감소시키는 서비스
 */
@Service
class RedissonAtomicCommandDeductService(
    val redissonClient: RedissonClient
) : StockDeductService {
    private val stockKey = "stock:remain_quantity"

    override fun supports(serviceType: ServiceType): Boolean {
        return ServiceType.REDISSON_ATOMIC_COMMAND == serviceType
    }

    override fun deduct(deductCommandDto: DeductCommandDto) {
        deductCommandDto.ids.forEach { id ->
            val remainQuantity = redissonClient.getAtomicLong("$stockKey:$id")

            while (true) {
                val currentValue = remainQuantity.get()

                if (currentValue <= 0) {
                    println("재고가 부족합니다.")
                    return
                }

                if (remainQuantity.compareAndSet(currentValue, currentValue - 1)) {
                    break
                }
            }
        }
    }
}
