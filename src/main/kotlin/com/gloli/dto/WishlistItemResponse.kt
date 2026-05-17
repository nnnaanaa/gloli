package com.gloli.dto

import com.gloli.domain.enums.Priority
import com.gloli.domain.enums.Status
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
    val status: Status,
    val imageUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?
)
