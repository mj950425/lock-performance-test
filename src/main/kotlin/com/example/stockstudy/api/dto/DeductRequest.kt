package com.example.stockstudy.api.dto

import com.example.stockstudy.enums.ServiceType

data class DeductRequest(
    val type: ServiceType,
    val ids: List<Long>
)
