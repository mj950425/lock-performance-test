package com.example.stockstudy.service.redis

import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.enums.ServiceType
import com.example.stockstudy.service.StockDeductService
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

/***
 * 남은 재고량을 Redis에서 관리하고 Redisson Lua Script를 사용하여 재고량을 감소시키는 서비스
 */
@Service
class RedisWithLuaScriptDeductService(
    val redissonClient: RedissonClient
) : StockDeductService {
    private val stockKey = "stock:remain_quantity"
    private val luaScript = """
    local remainQuantityStr = redis.call('GET', KEYS[1])
    if remainQuantityStr == false then
        return "nil value"
    end
    local remainQuantity = tonumber(remainQuantityStr)
    if remainQuantity > 0 then
        redis.call('DECRBY', KEYS[1], ARGV[1])
        return tonumber(redis.call('GET', KEYS[1]))
    else
        return -1
    end
"""

    override fun supports(serviceType: ServiceType): Boolean {
        return ServiceType.LUA == serviceType
    }

    override fun deduct(deductCommandDto: DeductCommandDto) {
        val stockKeys = deductCommandDto.ids.map {
            "$stockKey:$it"
        }

        val result = redissonClient.script.eval<Any>(
            RScript.Mode.READ_WRITE,
            luaScript,
            RScript.ReturnType.VALUE,
            stockKeys,
            1
        )

        if (result == -1L) {
            println("재고가 부족합니다.")
        }
    }
}
