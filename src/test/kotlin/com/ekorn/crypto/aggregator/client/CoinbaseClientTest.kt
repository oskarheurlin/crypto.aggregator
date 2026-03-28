package com.ekorn.crypto.aggregator.client

import com.ekorn.crypto.aggregator.config.AppProperties
import com.ekorn.crypto.aggregator.config.AppProperties.SymbolEntry
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class CoinbaseClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: CoinbaseClient

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        val props =
            AppProperties(
                symbols = listOf(SymbolEntry("BTC-USD", "coinbase")),
                coinbase =
                    AppProperties.CoinbaseProperties(
                        baseUrl = mockServer.url("/").toString().trimEnd('/'),
                        timeoutMs = 5000,
                    ),
            )
        client = CoinbaseClient(props)
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `fetchTicker returns TickerPrice on success`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("""{"price": "67542.01"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = client.fetchTicker("BTC-USD")

            assertEquals("BTC-USD", result.symbol)
            assertEquals(BigDecimal("67542.01"), result.price)

            val request = mockServer.takeRequest()
            assertEquals("/products/BTC-USD/ticker", request.path)
        }

    @Test
    fun `fetchTicker throws on server error`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(500))

            assertThrows<Exception> {
                client.fetchTicker("BTC-USD")
            }
        }

    @Test
    fun `fetchProducts returns set of product IDs`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("""[{"id": "BTC-USD"}, {"id": "ETH-USD"}, {"id": "ETH-BTC"}]""")
                    .addHeader("Content-Type", "application/json"),
            )

            val products = client.fetchProducts()

            assertEquals(setOf("BTC-USD", "ETH-USD", "ETH-BTC"), products)
        }

    @Test
    fun `fetchProducts throws on server error`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(500))

            assertThrows<Exception> {
                client.fetchProducts()
            }
        }

    @Test
    fun `TickerResponse defaults to empty price`() {
        val response = CoinbaseClient.TickerResponse()
        assertEquals("", response.price)
    }

    @Test
    fun `ProductResponse defaults to empty id`() {
        val response = CoinbaseClient.ProductResponse()
        assertEquals("", response.id)
    }
}
