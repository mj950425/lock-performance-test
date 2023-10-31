package com.example.stockstudy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Version

@Entity
class StockWithOptimisticLock(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,
    @Column(name = "remain_quantity")
    var remainQuantity: Long,
    @Version
    var version: Int = 0
) {
    fun deduct() {
        remainQuantity -= 1
    }
}
