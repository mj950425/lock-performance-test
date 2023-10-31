package com.example.stockstudy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Stock(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,
    @Column(name = "remain_quantity")
    var remainQuantity: Long
) {
    fun deduct() {
        remainQuantity -= 1
    }
}
