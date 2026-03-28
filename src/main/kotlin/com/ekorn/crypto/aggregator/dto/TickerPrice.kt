package com.ekorn.crypto.aggregator.dto

import java.math.BigDecimal

data class TickerPrice(
    val symbol: String,
    val price: BigDecimal,
)