package com.ekorn.crypto.aggregator.dto

import java.math.BigDecimal
import java.time.Instant

data class PriceSnapshot(
    val symbol: String,
    val price: BigDecimal,
    val updatedAt: Instant,
)
