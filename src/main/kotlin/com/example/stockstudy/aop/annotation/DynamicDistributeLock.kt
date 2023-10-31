package com.example.stockstudy.aop.annotation

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DynamicDistributeLock(
    val key: String,
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
    val waitTime: Long = 5,
    val leaseTime: Long = 3L
)
