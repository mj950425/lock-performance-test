package com.example.stockstudy.repository

import com.example.stockstudy.entity.StockWithOptimisticLock
import org.springframework.data.jpa.repository.JpaRepository

interface StockWithOptimisticRepository : JpaRepository<StockWithOptimisticLock, Long> {
    fun findAllByIdIn(ids: List<Long>): List<StockWithOptimisticLock>
}
