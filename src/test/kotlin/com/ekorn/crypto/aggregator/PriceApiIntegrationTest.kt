package com.ekorn.crypto.aggregator

import com.ekorn.crypto.aggregator.client.ExchangeClient
import com.ekorn.crypto.aggregator.client.ExchangeClientRouter
import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import com.ekorn.crypto.aggregator.dto.TickerPrice
import com.ekorn.crypto.aggregator.store.PriceStore
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PriceApiIntegrationTest {
    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun stubExchangeClientRouter(): ExchangeClientRouter {
            val stubClient =
                object : ExchangeClient {
                    override val exchangeName = "stub"

                    override suspend fun fetchTicker(symbol: String): TickerPrice {
                        if (symbol == "ETH-BTC") throw RuntimeException("Stub: skipping ETH-BTC for test")
                        return TickerPrice(symbol, BigDecimal("1.00"))
                    }

                    override suspend fun fetchProducts() = setOf("BTC-USD", "ETH-USD", "ETH-BTC")
                }
            return ExchangeClientRouter(
                appProperties =
                    com.ekorn.crypto.aggregator.config.AppProperties(
                        symbols =
                            listOf(
                                com.ekorn.crypto.aggregator.config.AppProperties
                                    .SymbolEntry("BTC-USD", "stub"),
                                com.ekorn.crypto.aggregator.config.AppProperties
                                    .SymbolEntry("ETH-USD", "stub"),
                                com.ekorn.crypto.aggregator.config.AppProperties
                                    .SymbolEntry("ETH-BTC", "stub"),
                            ),
                    ),
                clients = listOf(stubClient),
            )
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var priceStore: PriceStore

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `full stack returns price from store`() {
        priceStore.put(
            "BTC-USD",
            PriceSnapshot(
                symbol = "BTC-USD",
                price = BigDecimal("67542.01"),
                updatedAt = Instant.parse("2026-03-28T12:00:00Z"),
            ),
        )

        webTestClient
            .get()
            .uri("/prices/BTC-USD")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.symbol")
            .isEqualTo("BTC-USD")
            .jsonPath("$.price")
            .isEqualTo(67542.01)
            .jsonPath("$.timestamp")
            .isEqualTo("2026-03-28T12:00:00Z")
    }

    @Test
    fun `full stack returns 404 for untracked symbol`() {
        webTestClient
            .get()
            .uri("/prices/DOGE-USD")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(404)
            .jsonPath("$.error")
            .isEqualTo("Not Found")
            .jsonPath("$.message")
            .value<String> { msg ->
                assert(msg.contains("DOGE-USD"))
                assert(msg.contains("BTC-USD"))
            }
    }

    @Test
    fun `full stack returns 404 when no price available yet`() {
        webTestClient
            .get()
            .uri("/prices/ETH-BTC")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.message")
            .value<String> { msg ->
                assert(msg.contains("No price available yet"))
                assert(msg.contains("ETH-BTC"))
            }
    }

    @Test
    fun `full stack handles case-insensitive symbol`() {
        priceStore.put(
            "ETH-USD",
            PriceSnapshot(
                symbol = "ETH-USD",
                price = BigDecimal("2000.50"),
                updatedAt = Instant.parse("2026-03-28T12:00:00Z"),
            ),
        )

        webTestClient
            .get()
            .uri("/prices/eth-usd")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.symbol")
            .isEqualTo("ETH-USD")
            .jsonPath("$.price")
            .isEqualTo(2000.50)
    }
}
