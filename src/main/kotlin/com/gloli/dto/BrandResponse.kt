package com.gloli.dto

/** ブランド取得レスポンス */
data class BrandResponse(
    val id: Long,
    val name: String,
    val url: String?
)
