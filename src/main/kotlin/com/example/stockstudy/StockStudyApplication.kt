package com.example.stockstudy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StockStudyApplication

fun main(args: Array<String>) {
    runApplication<StockStudyApplication>(*args)
}
