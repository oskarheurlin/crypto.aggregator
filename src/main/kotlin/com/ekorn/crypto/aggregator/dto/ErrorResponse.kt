package com.ekorn.crypto.aggregator.dto

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
)
