package com.ekorn.crypto.aggregator.controller

import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import com.ekorn.crypto.aggregator.service.NoPriceAvailableException
import com.ekorn.crypto.aggregator.service.PriceQueryService
import com.ekorn.crypto.aggregator.service.SymbolNotTrackedException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.Instant

class PriceControllerTest {
    private val priceQueryService = mock<PriceQueryService>()
    private val controller = PriceController(priceQueryService)

    private val webTestClient =
        WebTestClient
            .bindToController(controller)
            .controllerAdvice(controller)
            .build()

    @Test
    fun `returns 200 with price data for valid symbol`() {
        val snapshot = PriceSnapshot("BTC-USD", BigDecimal("67542.01"), Instant.parse("2026-03-28T12:00:00Z"))
        whenever(priceQueryService.getPrice("BTC-USD")).thenReturn(snapshot)

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
    fun `returns 404 for untracked symbol`() {
        whenever(priceQueryService.getPrice("DOGE-USD"))
            .thenThrow(SymbolNotTrackedException("DOGE-USD", setOf("BTC-USD", "ETH-USD")))

        webTestClient
            .get()
            .uri("/prices/DOGE-USD")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(404)
            .jsonPath("$.message")
            .isEqualTo("Symbol not tracked: DOGE-USD. Available: [BTC-USD, ETH-USD]")
    }

    @Test
    fun `returns 404 when no price available yet`() {
        whenever(priceQueryService.getPrice("ETH-BTC"))
            .thenThrow(NoPriceAvailableException("ETH-BTC"))

        webTestClient
            .get()
            .uri("/prices/ETH-BTC")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.message")
            .isEqualTo("No price available yet for: ETH-BTC")
    }
}
