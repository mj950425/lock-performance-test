package com.example.stockstudy.api

import com.example.stockstudy.api.dto.DeductRequest
import com.example.stockstudy.dto.DeductCommandDto
import com.example.stockstudy.service.StockDeductService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/deduct")
class StockDeductController(
    val stockDeductServices: List<StockDeductService>,
) {
    @PostMapping
    fun deduct(
        @RequestBody request: DeductRequest,
    ) {
        val service = stockDeductServices.find { it.supports(request.type) }!!

        service.deduct(
            DeductCommandDto(
                request.ids
            )
        )
    }
}
