package com.ekorn.crypto.aggregator.config

import org.springframework.stereotype.Component
import java.util.*

@Component
class TrackedSymbolsProvider(
    appProperties: AppProperties,
) {
    val symbols: LinkedHashSet<String> =
        appProperties.symbols
            .map { it.symbol.trim().uppercase(Locale.ROOT) }
            .toCollection(LinkedHashSet())
}
