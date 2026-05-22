package com.gloli.dto

data class BulkRefreshResult(
    val total: Int,
    val updated: Int,
    val failed: Int
)
