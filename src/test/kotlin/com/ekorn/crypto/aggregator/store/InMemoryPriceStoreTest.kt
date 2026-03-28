package com.ekorn.crypto.aggregator.store

import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class InMemoryPriceStoreTest {
    private val store = InMemoryPriceStore()

    @Test
    fun `get returns null for unknown symbol`() {
        assertNull(store.get("UNKNOWN"))
    }

    @Test
    fun `put and get returns stored snapshot`() {
        val snapshot = PriceSnapshot("BTC-USD", BigDecimal("67000.50"), Instant.parse("2026-03-28T12:00:00Z"))
        store.put("BTC-USD", snapshot)

        assertEquals(snapshot, store.get("BTC-USD"))
    }

    @Test
    fun `put overwrites previous value`() {
        val old = PriceSnapshot("BTC-USD", BigDecimal("60000"), Instant.parse("2026-03-28T12:00:00Z"))
        val updated = PriceSnapshot("BTC-USD", BigDecimal("70000"), Instant.parse("2026-03-28T12:00:10Z"))

        store.put("BTC-USD", old)
        store.put("BTC-USD", updated)

        assertEquals(updated, store.get("BTC-USD"))
    }
}
