package com.ekorn.crypto.aggregator.service

import com.ekorn.crypto.aggregator.config.AppProperties
import com.ekorn.crypto.aggregator.config.AppProperties.SymbolEntry
import com.ekorn.crypto.aggregator.config.TrackedSymbolsProvider
import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import com.ekorn.crypto.aggregator.store.InMemoryPriceStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

class PriceQueryServiceTest {
    private val store = InMemoryPriceStore()
    private val appProperties =
        AppProperties(
            symbols =
                listOf(
                    SymbolEntry("BTC-USD"),
                    SymbolEntry("ETH-USD"),
                    SymbolEntry("ETH-BTC"),
                ),
        )
    private val queryService = PriceQueryService(TrackedSymbolsProvider(appProperties), store)

    @Test
    fun `returns price for tracked symbol`() {
        val snapshot = PriceSnapshot("BTC-USD", BigDecimal("67000"), Instant.parse("2026-03-28T12:00:00Z"))
        store.put("BTC-USD", snapshot)

        val result = queryService.getPrice("BTC-USD")
        assertEquals(snapshot, result)
    }

    @Test
    fun `normalizes symbol input to uppercase`() {
        val snapshot = PriceSnapshot("BTC-USD", BigDecimal("67000"), Instant.parse("2026-03-28T12:00:00Z"))
        store.put("BTC-USD", snapshot)

        val result = queryService.getPrice("btc-usd")
        assertEquals(snapshot, result)
    }

    @Test
    fun `trims whitespace from symbol`() {
        val snapshot = PriceSnapshot("BTC-USD", BigDecimal("67000"), Instant.parse("2026-03-28T12:00:00Z"))
        store.put("BTC-USD", snapshot)

        val result = queryService.getPrice("  BTC-USD  ")
        assertEquals(snapshot, result)
    }

    @Test
    fun `throws SymbolNotTrackedException for untracked symbol`() {
        val exception =
            assertThrows<SymbolNotTrackedException> {
                queryService.getPrice("DOGE-USD")
            }
        assert(exception.message!!.contains("DOGE-USD"))
        assert(exception.message!!.contains("BTC-USD"))
    }

    @Test
    fun `throws NoPriceAvailableException for tracked symbol with no data`() {
        val exception =
            assertThrows<NoPriceAvailableException> {
                queryService.getPrice("ETH-BTC")
            }
        assert(exception.message!!.contains("ETH-BTC"))
    }
}
