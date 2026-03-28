package com.ekorn.crypto.aggregator.service

import com.ekorn.crypto.aggregator.client.ExchangeClientRouter
import com.ekorn.crypto.aggregator.config.TrackedSymbolsProvider
import com.ekorn.crypto.aggregator.dto.PriceSnapshot
import com.ekorn.crypto.aggregator.store.PriceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class PricePollingService(
    private val trackedSymbolsProvider: TrackedSymbolsProvider,
    private val exchangeClientRouter: ExchangeClientRouter,
    private val priceStore: PriceStore,
    private val clock: Clock,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(PricePollingService::class.java)

    override fun run(args: ApplicationArguments) {
        runBlocking {
            validateSymbols()
            fetchAll()
        }
    }

    @Scheduled(
        fixedDelayString = "\${app.fetch-interval-ms}",
        initialDelayString = "\${app.fetch-interval-ms}",
    )
    fun scheduledFetch() {
        runBlocking { fetchAll() }
    }

    suspend fun fetchAll() {
        val symbols = trackedSymbolsProvider.symbols
        logger.info("Fetching prices for {}", symbols)
        supervisorScope {
            symbols.forEach { symbol ->
                launch { fetchSymbol(symbol) }
            }
        }
    }

    private suspend fun fetchSymbol(symbol: String) {
        try {
            val ticker = exchangeClientRouter.fetchTicker(symbol)
            val snapshot =
                PriceSnapshot(
                    symbol = ticker.symbol,
                    price = ticker.price,
                    updatedAt = Instant.now(clock),
                )
            priceStore.put(symbol, snapshot)
            logger.debug("Updated {} = {}", symbol, ticker.price)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch price for {}: {}", symbol, e.message)
        }
    }

    private suspend fun validateSymbols() {
        val symbols = trackedSymbolsProvider.symbols
        logger.info("Validating tracked symbols against their exchanges...")
        for (symbol in symbols) {
            val availableProducts = exchangeClientRouter.fetchProducts(symbol)

            if (availableProducts.isEmpty()) {
                throw IllegalStateException("Failed to retrieve products for $symbol. Cannot validate.")
            }

            if (symbol !in availableProducts) {
                throw IllegalStateException("Symbol $symbol is not available on its configured exchange.")
            }
        }
        logger.info("All tracked symbols validated: {}", symbols)
    }
}
