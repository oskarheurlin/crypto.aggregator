package com.ekorn.crypto.aggregator.service

import com.ekorn.crypto.aggregator.config.TrackedSymbolsProvider
import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import com.ekorn.crypto.aggregator.store.PriceStore
import org.springframework.stereotype.Service
import java.util.*

@Service
class PriceQueryService(
    private val trackedSymbolsProvider: TrackedSymbolsProvider,
    private val priceStore: PriceStore,
) {
    fun getPrice(symbol: String): PriceSnapshot {
        val normalized = symbol.trim().uppercase(Locale.ROOT)

        if (normalized !in trackedSymbolsProvider.symbols) {
            throw SymbolNotTrackedException(normalized, trackedSymbolsProvider.symbols)
        }

        return priceStore.get(normalized) ?: throw NoPriceAvailableException(normalized)
    }
}

class SymbolNotTrackedException(
    symbol: String,
    tracked: Set<String>,
) : RuntimeException("Symbol not tracked: $symbol. Available: $tracked")

class NoPriceAvailableException(
    symbol: String,
) : RuntimeException("No price available yet for: $symbol")
