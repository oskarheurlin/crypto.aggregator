package com.ekorn.crypto.aggregator.client

import com.ekorn.crypto.aggregator.dto.TickerPrice

interface ExchangeClient {
    val exchangeName: String

    suspend fun fetchTicker(symbol: String): TickerPrice

    suspend fun fetchProducts(): Set<String>
}
