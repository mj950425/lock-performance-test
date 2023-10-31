package com.example.stockstudy.service

import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.enums.ServiceType

interface StockDeductService {
    fun supports(serviceType: ServiceType): Boolean
    fun deduct(deductCommandDto: DeductCommandDto)
}
