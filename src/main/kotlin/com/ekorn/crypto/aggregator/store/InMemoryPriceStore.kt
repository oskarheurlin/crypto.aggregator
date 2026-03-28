package com.ekorn.crypto.aggregator.store

import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryPriceStore : PriceStore {
    private val prices = ConcurrentHashMap<String, PriceSnapshot>()

    override fun put(
        symbol: String,
        data: PriceSnapshot,
    ) {
        prices[symbol] = data
    }

    override fun get(symbol: String): PriceSnapshot? = prices[symbol]
}
