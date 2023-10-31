# 개요

Redisson의 분산락(multi lock)과 Optimistic Lock을 활용한 재고 차감 기능과 Pesimistic Lock을 활용한 재고 차감 기능에 대한 성능을 비교합니다.

# 코드

## Redisson multi lock과 Optimistic Lock

AOP와 SpringEL 활용해서 비지니스 로직과 락을 제어하는 코드를 분리했습니다.

동작 순서는 아래와 같습니다.

1. AOP가 DynamicDistribution 어노테이션을 발견합니다.
2. 프록시 클래스를 만듭니다.
3. SpringEL을 통해서 타겟 메소드의 시그니처를 기반으로 동적으로 락에 활용할 키를 만듭니다.
4. Redisson의 Multi Lock을 점유합니다.
5. 새로운 트랜잭션을 열고, 타겟 메소드를 실행합니다.
6. Multi Lock을 반환합니다.

### 서비스 코드

```kotlin
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
```

### AOP 코드

```kotlin
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
```

### SpringEL 코드

```kotlin
class CustomSpringELParser {
    companion object {
        private val parser = SpelExpressionParser()

        fun getDynamicKey(parameterNames: Array<String>, args: Array<Any>, key: String): Any {
            val context = StandardEvaluationContext().apply {
                for ((index, paramName) in parameterNames.withIndex()) {
                    setVariable(paramName, args[index])
                }
            }

            return parser.parseExpression(key).getValue(context, Any::class.java)!!
        }
    }
}
```

## Pessimistic Lock

### 서비스 코드

```kotlin
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
```

# 테스트 방법

테스트 방법은 아래와 같습니다.

1. docker compose를 통해서 먼저 데이터베이스와 레디스를 실행한다.
2. springboot 앱을 실행한다.
3. 자동으로 flyway로 데이터 마이그레이션이 이뤄진다.
4. jmeter를 설치하고 실행한다.
5. jmeter 디렉토리의 Thread Group.jmx를 open한다.
6. 테스트 환경을 설정하고, 원하는 쓰레드 그룹만을 enable한 뒤 실행시킨다.
7. aggregation result를 확인한다.

# 결과

| 종류                                   | 요청 방식                                    | 사양                                                                                                                                                                                                      | 첫번째 테스트                                                                                                                                  | 두번째 테스트                                                                                                                                        | 특이점                                            |
|--------------------------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| Redisson Multi Lock과 Optimisitc Lock | ids [1,2]에 대한 재고 차감과 ids [1,3]에 대한 재고 차감 | **사양**: MacBook Pro(16인치, 2021년)<br>**플랫폼**: macOS (ARM64 아키텍처)<br>**CPU 사용률**: 12.58%<br>**메모리 사용률**: 32.00GB 중 20.92GB 사용<br>**MYSQL 설정**: mysql 8.0.32, REPEATABLE-READ<br>**REDIS 설정**: redis 7.0.8 | **설정**<br>users: 200명, ramp-up: 10초, loop: 100회<br>**결과**<br>에러율 : 10번의 락 획득 실패. [1,2] 2번 실패, [1,3] 8번 실패<br>성능 결과 : throughput 122.3개/초 | **설정**<br>users: 1000명, ramp-up: 10초, loop: 100회<br>**결과**<br>에러율 : 722번의 락 획득 실패. [1,2] 349번 실패, [1,3] 373번 실패<br>성능 결과 : throughput 108.7개/초 | x                                              |
| Pessmistic Lock                      | ids [1,2]에 대한 재고 차감과 ids [1,3]에 대한 재고 차감 | **사양**: MacBook Pro(16인치, 2021년)<br>**플랫폼**: macOS (ARM64 아키텍처)<br>**CPU 사용률**: 11.08%<br>**메모리 사용률**: 32.00GB 중 21.45GB 사용<br>**MYSQL 설정**: mysql 8.0.32, REPEATABLE-READ<br>**REDIS 설정**: redis 7.0.8 | **설정**<br>users: 200명, ramp-up: 10초, loop: 100회<br>**결과**<br>에러율 : 0.00%<br>성능 결과 : throughput 320.6개/초                                  | **설정**<br>users: 1000명, ramp-up: 10초, loop: 100회<br>**결과**<br>에러율 : 0.00%<br>성능 결과 : throughput 266.8개/초                                       | 두번째 테스트에서 데이터 정합성이 1개 안맞음(1~3개씩 안맞는 경우가 종종 있음) |
