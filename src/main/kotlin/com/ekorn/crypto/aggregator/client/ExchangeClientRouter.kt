package com.ekorn.crypto.aggregator.client

import com.ekorn.crypto.aggregator.config.AppProperties
import com.ekorn.crypto.aggregator.dto.TickerPrice
import org.springframework.stereotype.Component
import java.util.*

@Component
class ExchangeClientRouter(
    appProperties: AppProperties,
    clients: List<ExchangeClient>,
) {
    private val clientsByName: Map<String, ExchangeClient> =
        clients.associateBy { it.exchangeName }

    private val symbolToExchange: Map<String, ExchangeClient> =
        appProperties.symbols.associate { entry ->
            val symbol = entry.symbol.trim().uppercase(Locale.ROOT)
            val exchange = entry.exchange.trim().lowercase(Locale.ROOT)
            val client =
                clientsByName[exchange]
                    ?: throw IllegalStateException("No client registered for exchange: $exchange (symbol: $symbol)")
            symbol to client
        }

    suspend fun fetchTicker(symbol: String): TickerPrice = getClient(symbol).fetchTicker(symbol)

    suspend fun fetchProducts(symbol: String): Set<String> = getClient(symbol).fetchProducts()

    private fun getClient(symbol: String): ExchangeClient =
        symbolToExchange[symbol]
            ?: throw IllegalArgumentException("No exchange configured for symbol: $symbol")
}
