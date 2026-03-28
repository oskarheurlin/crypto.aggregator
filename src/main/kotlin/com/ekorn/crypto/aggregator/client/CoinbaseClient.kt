package com.ekorn.crypto.aggregator.client

import com.ekorn.crypto.aggregator.config.AppProperties
import com.ekorn.crypto.aggregator.dto.TickerPrice
import io.netty.channel.ChannelOption
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Component
class CoinbaseClient(
    appProperties: AppProperties,
) : ExchangeClient {
    override val exchangeName = "coinbase"

    private val webClient: WebClient =
        WebClient
            .builder()
            .baseUrl(appProperties.coinbase.baseUrl)
            .defaultHeader("Accept", "application/json")
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient
                        .create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, appProperties.coinbase.timeoutMs.toInt())
                        .responseTimeout(Duration.ofMillis(appProperties.coinbase.timeoutMs)),
                ),
            ).codecs { it.defaultCodecs().maxInMemorySize(appProperties.coinbase.maxResponseSizeBytes) }
            .build()

    override suspend fun fetchTicker(symbol: String): TickerPrice {
        val response =
            webClient
                .get()
                .uri("/products/{productId}/ticker", symbol)
                .retrieve()
                .awaitBody<TickerResponse>()

        return TickerPrice(
            symbol = symbol,
            price = response.price.toBigDecimal(),
        )
    }

    override suspend fun fetchProducts(): Set<String> {
        val products =
            webClient
                .get()
                .uri("/products")
                .retrieve()
                .awaitBody<List<ProductResponse>>()

        return products.map { it.id }.toSet()
    }

    internal data class TickerResponse(
        val price: String = "",
    )

    internal data class ProductResponse(
        val id: String = "",
    )
}
