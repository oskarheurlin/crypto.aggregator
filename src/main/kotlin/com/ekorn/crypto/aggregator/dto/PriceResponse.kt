package com.ekorn.crypto.aggregator.dto

import java.math.BigDecimal
import java.time.Instant

data class PriceResponse(
    val symbol: String,
    val price: BigDecimal,
    val timestamp: Instant,
)
