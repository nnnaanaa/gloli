package com.mortlace.dto

import com.mortlace.domain.enums.Priority
import java.math.BigDecimal
import java.time.LocalDateTime

data class WishlistItemResponse(
    val id: Long,
    val name: String?,
    val url: String,
    val price: BigDecimal?,
    val brand: BrandResponse?,
    val category: CategoryResponse?,
    val notes: String?,
    val priority: Priority,
    val imageUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
