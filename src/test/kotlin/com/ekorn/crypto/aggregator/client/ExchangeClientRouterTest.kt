package com.ekorn.crypto.aggregator.client

import com.ekorn.crypto.aggregator.config.AppProperties
import com.ekorn.crypto.aggregator.config.AppProperties.SymbolEntry
import com.ekorn.crypto.aggregator.dto.TickerPrice
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class ExchangeClientRouterTest {
    private val coinbaseClient =
        mock<ExchangeClient>().apply {
            whenever(exchangeName).thenReturn("coinbase")
        }

    @Test
    fun `routes fetchTicker to correct exchange client`() =
        runTest {
            val router =
                buildRouter(
                    SymbolEntry("BTC-USD", "coinbase"),
                )
            whenever(coinbaseClient.fetchTicker("BTC-USD"))
                .thenReturn(TickerPrice("BTC-USD", BigDecimal("67000")))

            val result = router.fetchTicker("BTC-USD")

            assertEquals("BTC-USD", result.symbol)
            assertEquals(BigDecimal("67000"), result.price)
        }

    @Test
    fun `routes fetchProducts to correct exchange client`() =
        runTest {
            val router =
                buildRouter(
                    SymbolEntry("ETH-USD", "coinbase"),
                )
            whenever(coinbaseClient.fetchProducts())
                .thenReturn(setOf("ETH-USD", "BTC-USD"))

            val result = router.fetchProducts("ETH-USD")

            assertEquals(setOf("ETH-USD", "BTC-USD"), result)
        }

    @Test
    fun `normalizes symbol to uppercase`() =
        runTest {
            val router =
                buildRouter(
                    SymbolEntry("btc-usd", "coinbase"),
                )
            whenever(coinbaseClient.fetchTicker("BTC-USD"))
                .thenReturn(TickerPrice("BTC-USD", BigDecimal("67000")))

            val result = router.fetchTicker("BTC-USD")

            assertEquals(BigDecimal("67000"), result.price)
        }

    @Test
    fun `throws when symbol has no configured exchange`() {
        val router =
            buildRouter(
                SymbolEntry("BTC-USD", "coinbase"),
            )

        assertThrows<IllegalArgumentException> {
            runTest { router.fetchTicker("DOGE-USD") }
        }
    }

    @Test
    fun `throws at construction when exchange has no registered client`() {
        assertThrows<IllegalStateException> {
            buildRouter(
                SymbolEntry("BTC-USD", "binance"),
            )
        }
    }

    private fun buildRouter(vararg symbols: SymbolEntry): ExchangeClientRouter {
        val props = AppProperties(symbols = symbols.toList())
        return ExchangeClientRouter(props, listOf(coinbaseClient))
    }
}
