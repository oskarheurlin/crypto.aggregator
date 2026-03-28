package com.ekorn.crypto.aggregator.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    @field:NotEmpty
    val symbols: List<SymbolEntry>,
    @field:Positive
    val fetchIntervalMs: Long = 10000,
    @field:Valid
    val coinbase: CoinbaseProperties = CoinbaseProperties(),
) {
    data class SymbolEntry(
        @field:NotBlank
        val symbol: String,
        @field:NotBlank
        val exchange: String = "coinbase",
    )

    data class CoinbaseProperties(
        @field:NotBlank
        val baseUrl: String = "https://api.exchange.coinbase.com",
        @field:Positive
        val timeoutMs: Long = 5000,
        @field:Positive
        val maxResponseSizeBytes: Int = 10 * 1024 * 1024,
    )
}
