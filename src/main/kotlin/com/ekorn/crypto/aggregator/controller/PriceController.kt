package com.ekorn.crypto.aggregator.controller

import com.ekorn.crypto.aggregator.dto.ErrorResponse
import com.ekorn.crypto.aggregator.dto.PriceResponse
import com.ekorn.crypto.aggregator.service.NoPriceAvailableException
import com.ekorn.crypto.aggregator.service.PriceQueryService
import com.ekorn.crypto.aggregator.service.SymbolNotTrackedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PriceController(
    private val priceQueryService: PriceQueryService,
) {
    @GetMapping("/prices/{symbol}")
    fun getPrice(
        @PathVariable symbol: String,
    ): PriceResponse {
        val snapshot = priceQueryService.getPrice(symbol)
        return PriceResponse(
            symbol = snapshot.symbol,
            price = snapshot.price,
            timestamp = snapshot.updatedAt,
        )
    }

    @ExceptionHandler(SymbolNotTrackedException::class, NoPriceAvailableException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(),
                error = HttpStatus.NOT_FOUND.reasonPhrase,
                message = ex.message!!,
            ),
        )
}
