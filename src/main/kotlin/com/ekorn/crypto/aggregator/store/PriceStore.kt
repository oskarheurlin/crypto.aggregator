package com.ekorn.crypto.aggregator.store

import com.ekorn.crypto.aggregator.dto.PriceSnapshot

interface PriceStore {
    fun put(
        symbol: String,
        data: PriceSnapshot,
    )

    fun get(symbol: String): PriceSnapshot?
}
