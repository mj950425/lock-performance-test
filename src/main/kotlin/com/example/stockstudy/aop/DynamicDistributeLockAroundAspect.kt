package com.example.stockstudy.aop

import com.example.stockstudy.aop.annotation.DynamicDistributeLock
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

@Aspect
@Component
class DynamicDistributeLockAroundAspect(
    private val redissonClient: RedissonClient,
    private val distributeLockTransaction: DistributeLockTransactionProxy
) {
    companion object {
        private const val REDISSON_LOCK_PREFIX = "EXPERIENCE:LOCK:"
    }

    @Around("@annotation(com.example.stockstudy.aop.annotation.DynamicDistributeLock)")
    fun acquireDistributeLockAndCallMethod(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature

        val lockMeta = signature.method.getAnnotation(DynamicDistributeLock::class.java)

        val dynamicKeys = getDynamicKeys(joinPoint, lockMeta.key)

        val rLocks = dynamicKeys.map {
            redissonClient.getLock(REDISSON_LOCK_PREFIX + it)
        }

        val multiLock = redissonClient.getMultiLock(*rLocks.toTypedArray())

        return tryLockAndProceed(joinPoint, multiLock, lockMeta, dynamicKeys)
    }

    private fun tryLockAndProceed(
        joinPoint: ProceedingJoinPoint,
        multiLock: RLock,
        lockMeta: DynamicDistributeLock,
        dynamicKeys: List<Long>
    ): Any? {
        val lockAcquired = multiLock.tryLock(lockMeta.waitTime, lockMeta.leaseTime, lockMeta.timeUnit)
        if (!lockAcquired) {
            throw Exception("[$dynamicKeys] redis lock 획득에 실패했습니다")
        }

        return try {
            distributeLockTransaction.proceed(joinPoint)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } finally {
            multiLock.unlock()
        }
    }

    private fun getDynamicKeys(joinPoint: ProceedingJoinPoint, keyExpression: String): List<Long> {
        val methodSignature = joinPoint.signature as? MethodSignature
            ?: throw IllegalArgumentException("메소드 시그니처 정보를 찾을 수 없습니다")

        val dynamicKey = CustomSpringELParser.getDynamicKey(
            methodSignature.parameterNames,
            joinPoint.args,
            keyExpression
        )

        return dynamicKey as? List<Long>
            ?: throw IllegalArgumentException("다이나믹키의 결과는 List<Long> 타입이어야 합니다")
    }
}
