package com.ekorn.crypto.aggregator.service

import com.ekorn.crypto.aggregator.client.ExchangeClientRouter
import com.ekorn.crypto.aggregator.config.AppProperties
import com.ekorn.crypto.aggregator.config.AppProperties.SymbolEntry
import com.ekorn.crypto.aggregator.config.TrackedSymbolsProvider
import com.ekorn.crypto.aggregator.dto.TickerPrice
import com.ekorn.crypto.aggregator.store.InMemoryPriceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PricePollingServiceTest {
    private val fixedInstant = Instant.parse("2026-03-28T12:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val router = mock<ExchangeClientRouter>()
    private val store = InMemoryPriceStore()
    private val appProperties =
        AppProperties(
            symbols =
                listOf(
                    SymbolEntry("BTC-USD", "coinbase"),
                    SymbolEntry("ETH-USD", "coinbase"),
                ),
        )
    private val trackedSymbolsProvider = TrackedSymbolsProvider(appProperties)

    private val service = PricePollingService(trackedSymbolsProvider, router, store, clock)

    @Test
    fun `normalizes symbols to uppercase`() {
        val props =
            AppProperties(
                symbols =
                    listOf(
                        SymbolEntry("btc-usd", "coinbase"),
                        SymbolEntry(" eth-usd ", "coinbase"),
                    ),
            )
        val provider = TrackedSymbolsProvider(props)

        assertEquals(linkedSetOf("BTC-USD", "ETH-USD"), provider.symbols)
    }

    @Test
    fun `appProperties exposes fetch interval`() {
        assertEquals(10000, appProperties.fetchIntervalMs)
    }

    @Test
    fun `fetchAll stores snapshots with correct timestamp`() =
        runTest {
            whenever(router.fetchTicker("BTC-USD"))
                .thenReturn(TickerPrice("BTC-USD", BigDecimal("67000")))
            whenever(router.fetchTicker("ETH-USD"))
                .thenReturn(TickerPrice("ETH-USD", BigDecimal("3500")))

            service.fetchAll()

            val btc = store.get("BTC-USD")
            assertNotNull(btc)
            assertEquals(BigDecimal("67000"), btc!!.price)
            assertEquals(fixedInstant, btc.updatedAt)

            val eth = store.get("ETH-USD")
            assertNotNull(eth)
            assertEquals(BigDecimal("3500"), eth!!.price)
        }

    @Test
    fun `scheduledFetch delegates to fetchAll`() {
        runBlocking {
            whenever(router.fetchTicker("BTC-USD"))
                .thenReturn(TickerPrice("BTC-USD", BigDecimal("67000")))
            whenever(router.fetchTicker("ETH-USD"))
                .thenReturn(TickerPrice("ETH-USD", BigDecimal("3500")))
        }

        service.scheduledFetch()

        assertNotNull(store.get("BTC-USD"))
        assertNotNull(store.get("ETH-USD"))
    }

    @Test
    fun `validateSymbols succeeds when all symbols are available`() =
        runTest {
            whenever(router.fetchProducts("BTC-USD"))
                .thenReturn(setOf("BTC-USD", "ETH-USD"))
            whenever(router.fetchProducts("ETH-USD"))
                .thenReturn(setOf("BTC-USD", "ETH-USD"))
            whenever(router.fetchTicker("BTC-USD"))
                .thenReturn(TickerPrice("BTC-USD", BigDecimal("67000")))
            whenever(router.fetchTicker("ETH-USD"))
                .thenReturn(TickerPrice("ETH-USD", BigDecimal("3500")))

            service.run(mock())
        }

    @Test
    fun `validateSymbols fails when symbol is not available on exchange`() {
        runBlocking {
            whenever(router.fetchProducts("BTC-USD"))
                .thenReturn(setOf("BTC-USD"))
            whenever(router.fetchProducts("ETH-USD"))
                .thenReturn(setOf("BTC-USD"))
        }

        val exception =
            assertThrows<IllegalStateException> {
                service.run(mock())
            }
        assert(exception.message!!.contains("ETH-USD"))
    }

    @Test
    fun `validateSymbols fails when products API returns empty`() {
        runBlocking {
            whenever(router.fetchProducts("BTC-USD"))
                .thenReturn(emptySet())
        }

        assertThrows<IllegalStateException> {
            service.run(mock())
        }
    }

    @Test
    fun `fetchSymbol rethrows CancellationException instead of swallowing it`() =
        runTest {
            whenever(router.fetchTicker("BTC-USD"))
                .thenAnswer { throw CancellationException("Job cancelled") }
            whenever(router.fetchTicker("ETH-USD"))
                .thenReturn(TickerPrice("ETH-USD", BigDecimal("3500")))

            service.fetchAll()

            assertNull(store.get("BTC-USD"))
            assertNotNull(store.get("ETH-USD"))
        }

    @Test
    fun `fetchAll continues when one symbol fails`() =
        runTest {
            whenever(router.fetchTicker("BTC-USD"))
                .thenThrow(RuntimeException("Connection failed"))
            whenever(router.fetchTicker("ETH-USD"))
                .thenReturn(TickerPrice("ETH-USD", BigDecimal("3500")))

            service.fetchAll()

            assertNull(store.get("BTC-USD"))
            assertNotNull(store.get("ETH-USD"))
        }
}
