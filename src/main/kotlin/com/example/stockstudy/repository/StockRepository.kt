package com.example.stockstudy.repository

import com.example.stockstudy.entity.Stock
import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<Stock, Long>
