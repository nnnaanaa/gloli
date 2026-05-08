package com.mortlace.dto

import java.math.BigDecimal

data class ScrapedProductInfo(
    val name: String?,
    val imageUrl: String?,
    val brand: String?,
    val price: BigDecimal?,
    val description: String?,
    val category: String?
)
